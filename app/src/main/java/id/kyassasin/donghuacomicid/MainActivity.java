package id.kyassasin.donghuacomicid;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import android.text.TextUtils;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String BASE = "https://anichin.moe";
    private static final int BG = Color.rgb(8,10,17), PANEL = Color.rgb(19,23,35);
    private static final int TEXT = Color.rgb(244,246,255), MUTED = Color.rgb(153,163,184);
    private final OkHttpClient http = new OkHttpClient.Builder().followRedirects(true).build();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout root, body, topBar;
    private EditText searchBox;
    private ExoPlayer player;
    private PlayerView playerView;
    private String currentTitle = "KyassasinDev";
    private final ArrayList<Show> cached = new ArrayList<>();

    static class Show {
        String title, url, image, meta;
        Show(String t,String u,String i,String m){title=t;url=u;image=i;meta=m;}
    }
    static class Episode {
        String title,url;
        Episode(String t,String u){title=t;url=u;}
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        showHome();
    }

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void shell(String title, boolean back){
        root=column();root.setBackgroundColor(BG);
        topBar=row();topBar.setPadding(dp(16),dp(14),dp(16),dp(12));
        TextView brand=label(back?"‹  "+title:"KYASSASINDEV",20,TEXT,true);
        topBar.addView(brand,new LinearLayout.LayoutParams(0,dp(42),1));
        if(!back){
            TextView search=label("⌕",27,TEXT,true);search.setGravity(Gravity.CENTER);
            search.setBackground(shape(PANEL,14));topBar.addView(search,new LinearLayout.LayoutParams(dp(44),dp(42)));
            search.setOnClickListener(v->showSearch());
        } else brand.setOnClickListener(v->showHome());
        root.addView(topBar);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        body=column();body.setPadding(dp(16),dp(8),dp(16),dp(28));scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    private void heading(String s){
        TextView t=label(s,19,TEXT,true);t.setPadding(0,dp(18),0,dp(12));body.addView(t);
    }
    private void message(String s){
        TextView t=label(s,14,MUTED,false);t.setPadding(dp(14),dp(16),dp(14),dp(16));
        t.setBackground(shape(PANEL,14));body.addView(t,new LinearLayout.LayoutParams(-1,-2));
    }
    private void showHome(){
        shell("Beranda",false);
        TextView hero=label("Jelajahi dunia donghua.",24,TEXT,true);
        hero.setPadding(dp(2),dp(10),dp(2),dp(5));body.addView(hero);
        TextView sub=label("Katalog, episode, dan pemutar dalam satu aplikasi.",13,MUTED,false);
        body.addView(sub);
        heading("Donghua terbaru");
        message("Menghubungkan ke katalog…");
        loadCatalog(BASE+"/",false);
    }
    private void showSearch(){
        shell("Pencarian",true);
        searchBox=new EditText(this);searchBox.setSingleLine(true);searchBox.setTextColor(TEXT);
        searchBox.setHintTextColor(MUTED);searchBox.setHint("Cari judul donghua…");
        searchBox.setPadding(dp(14),0,dp(14),0);searchBox.setBackground(shape(PANEL,13));
        body.addView(searchBox,new LinearLayout.LayoutParams(-1,dp(50)));
        Button go=button("Cari");body.addView(go,new LinearLayout.LayoutParams(-1,dp(48)));
        go.setOnClickListener(v->{String q=searchBox.getText().toString().trim();if(!q.isEmpty())loadCatalog(BASE+"/?s="+android.net.Uri.encode(q),true);});
        searchBox.setOnEditorActionListener((v,a,e)->{go.performClick();return true;});
    }
    private Button button(String text){
        Button b=new Button(this);b.setText(text);b.setTextColor(Color.WHITE);b.setAllCaps(false);
        b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(shape(Color.rgb(116,86,226),12));return b;
    }
    private void loadCatalog(String url,boolean clear){
        io.execute(()->{
            try{
                Document doc=get(url);ArrayList<Show> items=parseShows(doc);
                runOnUiThread(()->{
                    if(isFinishing())return;
                    if(clear)body.removeAllViews();
                    if(items.isEmpty()){message("Katalog belum dapat dimuat. Server sumber mungkin membatasi koneksi aplikasi. Coba lagi nanti.");return;}
                    cached.clear();cached.addAll(items);
                    if(!clear)body.removeViewAt(body.getChildCount()-1);
                    for(Show s:items)addShow(s);
                });
            }catch(Exception e){runOnUiThread(()->message("Gagal memuat katalog. Periksa koneksi internet lalu coba lagi."));}
        });
    }
    private Document get(String url)throws IOException{
        Request req=new Request.Builder().url(url).header("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
            .header("Accept-Language","id-ID,id;q=0.9,en;q=0.7").build();
        try(Response r=http.newCall(req).execute()){
            if(!r.isSuccessful()||r.body()==null)throw new IOException("HTTP "+r.code());
            return Jsoup.parse(r.body().string(),url);
        }
    }
    private ArrayList<Show> parseShows(Document doc){
        LinkedHashMap<String,Show> map=new LinkedHashMap<>();
        Elements cards=doc.select(".bsx, .listupd .bs, .page-item-detail, article");
        for(Element card:cards){
            Element a=card.selectFirst("a[href]");if(a==null)continue;
            String href=a.absUrl("href");if(!href.startsWith("http"))continue;
            Element titleEl=card.selectFirst(".tt, .entry-title, h2, h3, [title]");
            String title=titleEl!=null?titleEl.text():a.attr("title");
            if(title.isEmpty())title=a.text();if(title.isEmpty())continue;
            Element img=card.selectFirst("img");
            String image=img==null?"":firstNonEmpty(img.attr("data-src"),img.attr("data-lazy-src"),img.absUrl("src"));
            Element meta=card.selectFirst(".epx,.status,.typez,.year");
            map.put(href,new Show(title,href,image,meta==null?"Donghua":meta.text()));
        }
        return new ArrayList<>(map.values());
    }
    private String firstNonEmpty(String...v){for(String s:v)if(s!=null&&!s.trim().isEmpty())return s;return "";}
    private void addShow(Show s){
        LinearLayout card=row();card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(shape(PANEL,15));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(124));cp.bottomMargin=dp(10);body.addView(card,cp);
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);poster.setBackground(shape(Color.rgb(35,39,55),10));
        card.addView(poster,new LinearLayout.LayoutParams(dp(78),dp(104)));
        if(!s.image.isEmpty())loadImage(s.image,poster);
        LinearLayout info=column();info.setPadding(dp(12),0,0,0);card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=label(s.title,15,TEXT,true);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);info.addView(title);
        TextView meta=label(s.meta,12,MUTED,false);meta.setPadding(0,dp(6),0,dp(8));info.addView(meta);
        TextView open=label("Lihat detail  ›",12,Color.rgb(184,163,255),true);info.addView(open);
        card.setOnClickListener(v->showDetail(s));
    }
    private void loadImage(String url,ImageView target){
        io.execute(()->{try{Request q=new Request.Builder().url(url).build();try(Response r=http.newCall(q).execute()){
            if(r.body()==null)return;byte[] data=r.body().bytes();android.graphics.Bitmap bmp=android.graphics.BitmapFactory.decodeByteArray(data,0,data.length);
            if(bmp!=null)runOnUiThread(()->target.setImageBitmap(bmp));
        }}catch(Exception ignored){}});
    }
    private void showDetail(Show show){
        shell(show.title,true);
        heading(show.title);message("Memuat sinopsis dan daftar episode…");
        io.execute(()->{try{
            Document doc=get(show.url);Element desc=doc.selectFirst(".entry-content, .description, .desc, .entry-content-single");
            String synopsis=desc==null?"Sinopsis belum tersedia.":desc.text();
            Elements links=doc.select(".eplister a[href], .episodelist a[href], .episode-list a[href], a[href*=/episode/]");
            LinkedHashMap<String,Episode> eps=new LinkedHashMap<>();
            for(Element a:links){String u=a.absUrl("href"),t=a.text();if(u.startsWith("http")&&!t.isEmpty())eps.put(u,new Episode(t,u));}
            ArrayList<Episode> list=new ArrayList<>(eps.values());
            runOnUiThread(()->{
                body.removeAllViews();heading(show.title);message(synopsis);
                heading("Daftar episode");
                if(list.isEmpty()){message("Daftar episode tidak terbaca dari halaman sumber.");return;}
                for(Episode ep:list){TextView item=label("▶   "+ep.title,14,TEXT,true);item.setPadding(dp(14),dp(15),dp(14),dp(15));
                    item.setBackground(shape(PANEL,12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);body.addView(item,p);
                    item.setOnClickListener(v->resolveEpisode(ep));}
            });
        }catch(Exception e){runOnUiThread(()->{body.removeAllViews();message("Detail donghua gagal dimuat dari sumber.");});}});
    }
    private void resolveEpisode(Episode ep){
        shell(ep.title,true);heading(ep.title);message("Menyiapkan pemutar…");
        io.execute(()->{try{
            Document doc=get(ep.url);String media=findMedia(doc,ep.url,0);
            runOnUiThread(()->{
                body.removeAllViews();heading(ep.title);
                if(media==null){message("Sumber ini tidak menyediakan tautan video langsung yang kompatibel. Pemutar tidak akan memuat halaman iklan atau melewati proteksi video.");return;}
                startPlayer(media);
            });
        }catch(Exception e){runOnUiThread(()->{body.removeAllViews();message("Video gagal dimuat. Server mungkin memblokir permintaan aplikasi.");});}});
    }
    private String findMedia(Document doc,String base,int depth)throws IOException{
        Element video=doc.selectFirst("video source[src], video[src], source[src]");
        if(video!=null){String u=video.hasAttr("src")?video.absUrl("src"):video.parent().absUrl("src");if(isMedia(u))return u;}
        Elements sources=doc.select("iframe[src]");
        if(depth>=3)return null;
        for(Element frame:sources){
            String u=frame.absUrl("src");if(!u.startsWith("http"))continue;
            try{Document nested=get(u);String found=findMedia(nested,u,depth+1);if(found!=null)return found;}catch(Exception ignored){}
        }
        return null;
    }
    private boolean isMedia(String u){String x=u.toLowerCase(Locale.US);return x.contains(".m3u8")||x.contains(".mp4")||x.contains(".mpd");}
    private void startPlayer(String url){
        playerView=new PlayerView(this);playerView.setUseController(true);playerView.setBackgroundColor(Color.BLACK);
        body.addView(playerView,new LinearLayout.LayoutParams(-1,dp(220)));
        player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();
        TextView note=label("Pemutar native KyassasinDev • Media3",12,MUTED,false);note.setPadding(0,dp(10),0,0);body.addView(note);
    }
    @Override public void onBackPressed(){if(player!=null){player.release();player=null;}showHome();}
    @Override protected void onDestroy(){if(player!=null){player.release();player=null;}io.shutdownNow();super.onDestroy();}
}
