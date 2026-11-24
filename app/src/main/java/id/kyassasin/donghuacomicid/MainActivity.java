package id.kyassasin.donghuacomicid;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String API = "https://anichin-api-nine.vercel.app";
    private static final int BG=0xFF080A12, PANEL=0xFF121725, TEXT=0xFFF5F6FF, MUTED=0xFF9AA4BB, ACCENT=0xFF9875FF;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LinearLayout root, content, top;
    private ExoPlayer player;
    private PlayerView playerView;
    private String currentTitle="KyassasinDev";
    private int page=1;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        showHome();
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private void base(String title, boolean back){
        if(player!=null){player.release();player=null;}
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        setContentView(root);
        top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),dp(12),dp(16),dp(12)); top.setBackgroundColor(BG);
        root.addView(top,new LinearLayout.LayoutParams(-1,dp(58)));
        if(back){ TextView b=text("‹",34,TEXT,false); b.setGravity(Gravity.CENTER); top.addView(b,new LinearLayout.LayoutParams(dp(38),-1)); b.setOnClickListener(v->showHome());}
        TextView brand=text(title,20,TEXT,true); top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView mark=text("KYASSASINDEV",9,ACCENT,true); top.addView(mark);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16),dp(8),dp(16),dp(26)); scroll.addView(content);
    }
    private void section(String title){
        TextView t=text(title,18,TEXT,true); t.setPadding(0,dp(18),0,dp(12)); content.addView(t);
    }
    private void note(String msg){
        TextView t=text(msg,14,MUTED,false); t.setPadding(dp(12),dp(20),dp(12),dp(20)); content.addView(t);
    }
    private void request(String path, java.util.function.Consumer<JSONObject> done, java.util.function.Consumer<String> fail){
        io.execute(()->{
            HttpURLConnection c=null;
            try{
                URL u=new URL(API+path); c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("GET");
                c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestProperty("Accept","application/json");
                c.setRequestProperty("User-Agent","KyassasinDev-Android/2.0");
                int status=c.getResponseCode(); java.io.InputStream in=status>=200&&status<300?c.getInputStream():c.getErrorStream();
                java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n;
                while((n=in.read(buf))!=-1)out.write(buf,0,n); in.close();
                JSONObject json=new JSONObject(out.toString("UTF-8"));
                runOnUiThread(()->done.accept(json));
            }catch(Exception e){runOnUiThread(()->fail.accept(e.getMessage()==null?"Koneksi gagal":e.getMessage()));}
            finally{if(c!=null)c.disconnect();}
        });
    }
    private void showHome(){
        base("KyassasinDev",false);
        TextView hero=text("DUNIA DONGHUA\nDALAM GENGGAMAN",25,TEXT,true); hero.setPadding(dp(18),dp(24),dp(18),dp(18));
        hero.setBackground(shape(0xFF19172B,18)); content.addView(hero);
        TextView sub=text("Temukan serial dan episode favoritmu.",13,MUTED,false); sub.setPadding(dp(18),0,dp(18),dp(18)); hero.setOnClickListener(v->showSearch()); content.addView(sub);
        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL); content.addView(actions);
        button(actions,"Cari Donghua",()->showSearch(),true); button(actions,"Ongoing",()->loadList("/ongoing?page=1","Sedang Tayang"),false);
        section("Episode Terbaru"); note("Memuat katalog...");
        final int marker=content.getChildCount()-1;
        request("/latest?page=1",j->{content.removeViewAt(marker); renderCards(extractItems(j));},e->{content.removeViewAt(marker);note("Katalog belum tersedia: "+e);});
        request("/popular",j->{section("Populer");renderCards(extractItems(j));},e->{});
    }
    private void button(LinearLayout parent,String label,Runnable action,boolean primary){
        TextView b=text(label,13,TEXT,true); b.setGravity(Gravity.CENTER); b.setPadding(dp(15),dp(12),dp(15),dp(12));
        b.setBackground(shape(primary?ACCENT:PANEL,12)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1); p.setMargins(0,dp(4),dp(8),dp(4)); parent.addView(b,p); b.setOnClickListener(v->action.run());
    }
    private JSONArray extractItems(JSONObject j){
        JSONArray direct=findArray(j,"latest","popular","anime","result","items","lists","ongoing","data","series","slide","latest_release","popular_today","recommendation");
        if(direct!=null)return direct; return new JSONArray();
    }
    private JSONArray findArray(Object obj,String... keys){
        if(obj instanceof JSONObject){
            JSONObject o=(JSONObject)obj;
            for(String k:keys){Object v=o.opt(k); if(v instanceof JSONArray)return (JSONArray)v;}
            Iterator<String> it=o.keys();
            while(it.hasNext()){Object v=o.opt(it.next()); if(v instanceof JSONObject || v instanceof JSONArray){JSONArray a=findArray(v,keys);if(a!=null)return a;}}
        } else if(obj instanceof JSONArray){
            JSONArray a=(JSONArray)obj;
            for(int i=0;i<a.length();i++){JSONArray found=findArray(a.opt(i),keys);if(found!=null)return found;}
        }
        return null;
    }
    private String first(JSONObject o,String... keys){
        for(String k:keys){String v=o.optString(k,"");if(!v.isEmpty()&&!v.equals("null"))return v;}
        return "";
    }
    private String slug(JSONObject o){
        String s=first(o,"slug","anime_slug","series_slug","id");
        if(s.isEmpty()){String u=first(o,"url","link","permalink"); if(!u.isEmpty()){try{String p=Uri.parse(u).getLastPathSegment();if(p!=null)s=p;}catch(Exception ignored){}}}
        return s;
    }
    private void renderCards(JSONArray items){
        if(items==null||items.length()==0){note("Belum ada data dari sumber. Coba lagi nanti.");return;}
        for(int i=0;i<items.length();i++){
            JSONObject o=items.optJSONObject(i); if(o==null)continue;
            String title=first(o,"title","name","anime_title"); if(title.isEmpty())title="Donghua";
            String image=first(o,"thumbnail","thumb","image","poster","cover");
            final String cardTitle=title; String s=slug(o); String meta=first(o,"episode","latest_episode","type","status");
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setPadding(dp(9),dp(9),dp(9),dp(9)); card.setBackground(shape(PANEL,14));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(126)); cp.setMargins(0,0,0,dp(10)); content.addView(card,cp);
            ImageView cover=new ImageView(this); cover.setScaleType(ImageView.ScaleType.CENTER_CROP); cover.setBackground(shape(0xFF252B3C,10));
            card.addView(cover,new LinearLayout.LayoutParams(dp(82),-1)); if(!image.isEmpty())Glide.with(this).load(image).centerCrop().into(cover);
            LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(12),dp(5),dp(4),dp(5)); card.addView(info,new LinearLayout.LayoutParams(0,-1,1));
            TextView name=text(title,15,TEXT,true); name.setMaxLines(3); info.addView(name);
            if(!meta.isEmpty()){TextView m=text(meta,11,MUTED,false);m.setPadding(0,dp(7),0,0);info.addView(m);}
            TextView open=text("LIHAT DETAIL  ›",10,ACCENT,true); LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-2,-2);op.topMargin=dp(9);info.addView(open,op);
            card.setOnClickListener(v->{if(!s.isEmpty())showDetail(s,cardTitle);else Toast.makeText(this,"Detail tidak tersedia untuk item ini",Toast.LENGTH_SHORT).show();});
        }
    }
    private void loadList(String path,String title){
        base(title,true); note("Memuat...");
        request(path,j->{content.removeAllViews();renderCards(extractItems(j));},e->{content.removeAllViews();note("Tidak dapat memuat katalog: "+e);});
    }
    private void showSearch(){
        base("Pencarian",true);
        EditText input=new EditText(this); input.setSingleLine(true);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setHint("Cari judul donghua...");
        input.setPadding(dp(14),0,dp(14),0);input.setBackground(shape(PANEL,12));content.addView(input,new LinearLayout.LayoutParams(-1,dp(50)));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);content.addView(results);
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){
            String q=s.toString().trim(); if(q.length()<2)return; try{String encoded=URLEncoder.encode(q,"UTF-8");request("/search?q="+encoded+"&page=1",j->{results.removeAllViews();LinearLayout old=content;content=results;renderCards(extractItems(j));content=old;},e->{});
            }catch(Exception ignored){}
        }public void afterTextChanged(Editable e){}});
    }
    private void showDetail(String slug,String title){
        base(title,true); note("Memuat detail...");
        request("/detail/"+Uri.encode(slug),j->{
            content.removeAllViews();JSONObject data=j.optJSONObject("data");if(data==null)data=j;
            JSONObject detail=data.optJSONObject("detail");if(detail!=null)data=detail;
            String desc=first(data,"synopsis","description","desc");
            if(!desc.isEmpty()){TextView d=text(desc,13,MUTED,false);d.setPadding(0,0,0,dp(16));content.addView(d);}
            section("Daftar Episode");
            JSONArray eps=findArray(data,"episodes","episode_list","chapter","list");
            if(eps==null||eps.length()==0){note("Daftar episode tidak ditemukan pada respons sumber.");return;}
            for(int i=0;i<eps.length();i++){
                JSONObject ep=eps.optJSONObject(i);if(ep==null)continue;
                final String epTitle=first(ep,"title","name","episode_title").isEmpty()?"Episode "+(i+1):first(ep,"title","name","episode_title");
                String epSlug=slug(ep); if(epSlug.isEmpty())epSlug=first(ep,"episode_slug");
                final String selectedSlug=epSlug;
                TextView row=text("▶   "+epTitle,14,TEXT,true);row.setPadding(dp(14),dp(15),dp(12),dp(15));row.setBackground(shape(PANEL,11));
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,0,0,dp(7));content.addView(row,rp);
                final int number=i+1;row.setOnClickListener(v->{if(!selectedSlug.isEmpty())loadEpisode(selectedSlug,epTitle);else Toast.makeText(this,"Tautan episode tidak tersedia",Toast.LENGTH_SHORT).show();});
            }
        },e->{content.removeAllViews();note("Gagal memuat detail: "+e);});
    }
    private void loadEpisode(String slug,String title){
        base(title,true);note("Memuat sumber video...");
        request("/episode/"+Uri.encode(slug),j->{
            JSONObject data=j.optJSONObject("data");if(data==null)data=j;
            String direct=findVideo(data);
            if(direct.isEmpty()){
                content.removeAllViews();note("Sumber ini hanya menyediakan embed atau format yang belum didukung pemutar native. Tidak ada video langsung yang bisa diputar di ExoPlayer.");return;
            }
            content.removeAllViews();section(title);
            playerView=new PlayerView(this);playerView.setUseController(true);playerView.setBackgroundColor(Color.BLACK);
            content.addView(playerView,new LinearLayout.LayoutParams(-1,dp(230)));
            player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);
            player.setMediaItem(MediaItem.fromUri(direct));player.prepare();player.play();
            TextView status=text("Pemutar KyassasinDev • Native ExoPlayer",12,MUTED,false);status.setPadding(0,dp(12),0,0);content.addView(status);
        },e->{content.removeAllViews();note("Tidak dapat memuat video: "+e);});
    }
    private String findVideo(Object obj){
        if(obj instanceof JSONObject){
            JSONObject o=(JSONObject)obj;
            String v=first(o,"video_url","videoUrl","file","src","mp4","hls","stream_url","server_url","download_url","url");
            if(v.startsWith("https://")||v.startsWith("http://")){
                String low=v.toLowerCase();if(low.contains(".mp4")||low.contains(".m3u8"))return v;
            }
            Iterator<String> it=o.keys();while(it.hasNext()){String x=findVideo(o.opt(it.next()));if(!x.isEmpty())return x;}
        }else if(obj instanceof JSONArray){JSONArray a=(JSONArray)obj;for(int i=0;i<a.length();i++){String x=findVideo(a.opt(i));if(!x.isEmpty())return x;}}
        return "";
    }
    @Override public void onBackPressed(){if(player!=null){player.release();player=null;}showHome();}
    @Override protected void onDestroy(){if(player!=null)player.release();io.shutdownNow();super.onDestroy();}
}