package org.livingroom.tvhome;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.tv.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.util.*;
import java.text.SimpleDateFormat;

/** Offline native launcher. No background service, analytics, network or elevated permissions. */
public class MainActivity extends Activity {
    FrameLayout root, content, rail;
    View railShade;
    TextView clock, pageTitle;
    Nav[] nav = new Nav[5];
    ArrayList<Tile> tiles = new ArrayList<>();
    ArrayList<App> favorites = new ArrayList<>(), library = new ArrayList<>();
    HashMap<String,Drawable> icons = new HashMap<>(), banners = new HashMap<>();
    float sx=1, sy=1;
    int page=0, lastHome=0, lastLibrary=0;
    boolean expanded=false, reduced=false, changing=false, appsUnlocked=false, appsDirty=false;
    BroadcastReceiver packageReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        appsDirty=true;
        if(hasWindowFocus())refreshApps();
    }};
    private boolean userUnlocked() {
        return Build.VERSION.SDK_INT < 24 || ((UserManager)getSystemService(USER_SERVICE)).isUserUnlocked();
    }
    private SharedPreferences preferences() {
        Context storage = Build.VERSION.SDK_INT >= 24 ? createDeviceProtectedStorageContext() : this;
        return storage.getSharedPreferences("home", MODE_PRIVATE);
    }
    void refreshApps(){
        boolean wasNav=expanded&&appsUnlocked;
        int navIndex=page;
        for(int j=0;j<nav.length;j++)if(nav[j]!=null&&nav[j].hasFocus())navIndex=j;
        icons.clear();banners.clear();loadApps();showPage(page,!wasNav);
        if(wasNav)nav[navIndex].requestFocus();
        appsDirty=false;appsUnlocked=userUnlocked();
    }
    Handler handler=new Handler(Looper.getMainLooper());
    final String[] labels={"首页","应用","投屏","输入源","设置"};
    final int[] colors={0xff20252a,0xff34151a,0xff162d42,0xff4b2533,0xff4a301b,0xff123e40};
    final Runnable tick=new Runnable(){public void run(){clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));handler.postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    static class App { String pkg,name; App(String p,String n){pkg=p;name=n;} }
    int x(float n){return Math.round(n*sx);} int y(float n){return Math.round(n*sy);}
    @Override public void onCreate(Bundle b){super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        Point size=new Point();getWindowManager().getDefaultDisplay().getRealSize(size);sx=size.x/1920f;sy=size.y/1080f;
        reduced=preferences().getBoolean("reduced",false);
        lastHome=preferences().getInt("lastHome",0);
        loadApps();build();showPage(0,true);
        appsUnlocked=userUnlocked();
        if (Build.VERSION.SDK_INT >= 24) registerReceiver(packageReceiver,new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        IntentFilter packagesFilter=new IntentFilter();packagesFilter.addAction(Intent.ACTION_PACKAGE_ADDED);packagesFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);packagesFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);packagesFilter.addDataScheme("package");registerReceiver(packageReceiver,packagesFilter);
    }
    void loadApps(){
        favorites.clear(); library.clear();
        TreeMap<String,App> found=new TreeMap<>();
        for(String cat:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            Intent q=new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for(ResolveInfo r:getPackageManager().queryIntentActivities(q,0)){
                String p=r.activityInfo.packageName;
                if(p.equals(getPackageName()))continue;
                if(!found.containsKey(p))found.put(p,new App(p,r.loadLabel(getPackageManager()).toString()));
            }
        }
        library.addAll(found.values());
        Collections.sort(library,(a,b)->a.name.compareToIgnoreCase(b.name));
        favorites.addAll(library.subList(0, Math.min(6, library.size())));
    }
    Intent launchIntent(String p){Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(p);return i!=null?i:getPackageManager().getLaunchIntentForPackage(p);}
    void build(){
        root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        ImageView bg=new ImageView(this);bg.setScaleType(ImageView.ScaleType.CENTER_CROP);bg.setImageResource(R.drawable.wallpaper);root.addView(bg,new FrameLayout.LayoutParams(-1,-1));
        View bottom=new View(this);bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x00000000,0xbb000000}));root.addView(bottom,new FrameLayout.LayoutParams(-1,-1));
        content=new FrameLayout(this);root.addView(content,new FrameLayout.LayoutParams(-1,-1));
        clock=text("",29,Color.WHITE);clock.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);place(root,clock,1630,35,220,60);
        railShade=new View(this);railShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xe6101216,0xc4101216,0x00101216}));place(root,railShade,0,0,425,1080);railShade.setAlpha(0.30f);
        rail=new FrameLayout(this);place(root,rail,0,0,340,1080);rail.setClipChildren(false);
        TextView user=text("TV",32,Color.WHITE);user.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));user.setGravity(Gravity.CENTER);user.setBackground(round(0x603b414a,26));place(rail,user,40,65,54,54);
        TextView name=text("客厅",20,0xffe5e5e5);name.setGravity(Gravity.CENTER);place(rail,name,22,122,90,32);
        for(int i=0;i<nav.length;i++){nav[i]=new Nav(i);place(rail,nav[i],22,220+i*104,84,76);}
    }
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,y(size));t.setTextColor(color);t.setFontFeatureSettings("kern");t.setIncludeFontPadding(false);return t;}
    GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(x(radius));return d;}
    void place(FrameLayout parent,View v,int left,int top,int w,int h){FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(x(w),y(h));lp.leftMargin=x(left);lp.topMargin=y(top);parent.addView(v,lp);}
    void expand(boolean value){if(expanded==value){if(pageTitle!=null)pageTitle.setAlpha(value?0f:1f);return;}expanded=value;
        railShade.animate().cancel();railShade.animate().alpha(value?0.94f:0.30f).setDuration(reduced?0:(value?160:100)).start();
        if(pageTitle!=null){pageTitle.animate().cancel();pageTitle.animate().alpha(value?0f:1f).setDuration(reduced?0:120).start();}
        for(Nav n:nav){n.getLayoutParams().width=x(value?280:84);n.requestLayout();n.invalidate();}
    }
    void showPage(int target,boolean focus){
        changing=true;page=target;content.removeAllViews();tiles.clear();
        if(page==0){
            TextView title=text("今晚，看点喜欢的",56,Color.WHITE);pageTitle=title;title.setTypeface(Typeface.create("sans-serif-light",0));place(content,title,175,300,1280,90);
            TextView heading=text("常用应用",27,0xffeeeeee);place(content,heading,175,775,1200,50);
            for(int i=0;i<favorites.size();i++){Tile t=new Tile(favorites.get(i),i,true);tiles.add(t);place(content,t,165+i*282,815,270,205);}
        }else{
            View scrim=new View(this);scrim.setBackgroundColor(0x92090b0e);content.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
            TextView title=text("应用",52,Color.WHITE);pageTitle=title;title.setTypeface(Typeface.create("sans-serif-light",0));place(content,title,175,125,1200,80);
            ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);scroll.setPadding(x(8),y(12),x(8),y(20));scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.setFocusable(false);
            FrameLayout grid=new FrameLayout(this);grid.setClipChildren(false);scroll.addView(grid,new ScrollView.LayoutParams(-1,y(((library.size()+4)/5)*220+35)));
            place(content,scroll,160,240,1710,795);
            for(int i=0;i<library.size();i++){Tile t=new Tile(library.get(i),i,false);tiles.add(t);place(grid,t,(i%5)*334,(i/5)*220,310,205);}
        }
        for(Nav n:nav)n.invalidate();if(expanded&&pageTitle!=null)pageTitle.setAlpha(0f);changing=false;
        if(focus)focusContent();
    }
    void focusContent(){if(tiles.isEmpty()){nav[page].requestFocus();return;}int index=page==0?lastHome:lastLibrary;tiles.get(Math.min(Math.max(index,0),tiles.size()-1)).requestFocus();expand(false);}
    void launch(String pkg){Intent i=launchIntent(pkg);if(i==null){toast("应用暂不可用，请在应用页检查");return;}open(i);}
    void open(Intent i){try{startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){android.util.Log.w("LivingRoomTV","Unable to launch "+i,e);toast("暂时无法打开，请使用系统入口");}}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    void activateNav(int i){switch(i){case 0:showPage(0,true);break;case 1:showPage(1,true);break;case 2:if(launchIntent("com.ionitech.airscreen")!=null)launch("com.ionitech.airscreen");else toast("请使用电视自带的投屏功能，或先安装投屏接收应用");break;case 3:inputs();break;case 4:settings();break;}}
    void inputs(){
        ArrayList<TvInputInfo> list=new ArrayList<>();ArrayList<String> options=new ArrayList<>();
        try{TvInputManager m=(TvInputManager)getSystemService(TV_INPUT_SERVICE);if(m!=null)for(TvInputInfo in:m.getTvInputList())if(in.isPassthroughInput()&&(Build.VERSION.SDK_INT < 24 || !in.isHidden(this))){list.add(in);CharSequence label=Build.VERSION.SDK_INT >= 24 ? in.loadCustomLabel(this) : null;if(label==null)label=in.loadLabel(this);options.add(label.toString()+(m.getInputState(in.getId())==TvInputManager.INPUT_STATE_CONNECTED?" · 已连接":""));}}catch(Exception ignored){}
        if(list.isEmpty()){toast("请按遥控器上的信号源键切换输入");return;}
        showMenu(new AlertDialog.Builder(this).setTitle("输入源").setItems(options.toArray(new String[0]),(d,n)->
            open(new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(list.get(n).getId()))))
            .setNegativeButton("返回",null));
    }
    void stockHome() {
        PackageManager pm = getPackageManager();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> homes = pm.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY);
        ArrayList<ComponentName> entries = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (ResolveInfo candidate : homes) {
            ActivityInfo info = candidate.activityInfo;
            if (info.packageName.equals(getPackageName()) || !info.exported ||
                (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 ||
                (info.permission != null && getPackageManager().checkPermission(info.permission,getPackageName()) != PackageManager.PERMISSION_GRANTED)) continue;
            if (info.packageName.equals("com.android.settings") || info.packageName.equals("android") ||
                info.name.endsWith("FallbackHome")) continue;
            entries.add(new ComponentName(info.packageName, info.name));
            names.add(candidate.loadLabel(pm).toString());
        }
        if (entries.isEmpty()) {
            toast("请按主页键，或在电视设置里选择原厂桌面");
        } else if (entries.size() == 1) {
            open(home.setComponent(entries.get(0)));
        } else {
            showMenu(new AlertDialog.Builder(this).setTitle("打开系统桌面")
                .setItems(names.toArray(new String[0]), (d,n)->open(home.setComponent(entries.get(n))))
                .setNegativeButton("返回",null));
        }
    }
    void showMenu(AlertDialog.Builder builder){
        AlertDialog d=builder.create();
        d.setOnShowListener(dialog->{ListView list=d.getListView();if(list!=null){list.requestFocus();list.setSelection(0);}});
        d.show();
    }
    void settings(){String[] items={"电视系统设置","打开原厂桌面",reduced?"动态效果：已关闭":"动态效果：已开启","关于 Living Room TV"};
        showMenu(new AlertDialog.Builder(this).setTitle("设置").setItems(items,(d,n)->{
            if(n==0)open(new Intent(android.provider.Settings.ACTION_SETTINGS));
            if(n==1)stockHome();
            if(n==2){reduced=!reduced;preferences().edit().putBoolean("reduced",reduced).apply();toast(reduced?"已关闭动态效果":"已开启轻量动态效果");}
            if(n==3)new AlertDialog.Builder(this).setTitle("Living Room TV 1.1.1").setMessage("为你的电视定制。\n\n方向键选择，确定键打开。\n应用返回后保留上次位置。\n原厂桌面保留在设置中。\n\n无联网权限，无后台服务。").setPositiveButton("知道了",null).show();
        }).setNegativeButton("返回",null));
    }
    @Override public void onBackPressed(){if(page==1){showPage(0,true);}else if(!getIntent().hasCategory(Intent.CATEGORY_HOME)){finish();}else{focusContent();}}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.hasCategory(Intent.CATEGORY_HOME)){if(page!=0)showPage(0,true);else focusContent();}}
    @Override protected void onResume(){super.onResume();if(appsDirty||(!appsUnlocked&&userUnlocked()))refreshApps();handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(tick);preferences().edit().putInt("lastHome",lastHome).apply();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);unregisterReceiver(packageReceiver);super.onDestroy();}
    class Nav extends View {
        int index;Paint p=new Paint(3);
        Nav(int i){super(MainActivity.this);index=i;setId(View.generateViewId());setFocusable(true);setClickable(true);setContentDescription(labels[i]);setOnClickListener(v->activateNav(index));setOnFocusChangeListener((v,f)->{if(f)expand(true);invalidate();});}
        @Override public boolean onKeyDown(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_UP){nav[Math.max(0,index-1)].requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_DOWN){nav[Math.min(4,index+1)].requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_RIGHT){focusContent();return true;}if(key==KeyEvent.KEYCODE_DPAD_LEFT)return true;return super.onKeyDown(key,e);}
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName(Button.class.getName());info.setSelected(index==page);}
        @Override protected void onDraw(Canvas c){c.save();c.scale(sx,sy);boolean f=hasFocus();p.setStyle(Paint.Style.FILL);p.setColor(f?0xeeeeeeee:index==page?0x483e434a:Color.TRANSPARENT);c.drawRoundRect(4,3,(expanded?280:84)-4,73,22,22,p);p.setColor(f?0xff17191c:0xffe3e5e8);drawSymbol(c,p,index,42,38,30);if(expanded){p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(28);c.drawText(labels[index],96,48,p);}c.restore();}
    }
    void drawSymbol(Canvas c,Paint p,int type,float cx,float cy,float size){
        c.save();c.translate(cx-size/2,cy-size/2);float k=size/32;c.scale(k,k);p.setStrokeWidth(2.4f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(type==0){Path a=new Path();a.moveTo(2,14);a.lineTo(16,2);a.lineTo(30,14);a.lineTo(26,14);a.lineTo(26,29);a.lineTo(19,29);a.lineTo(19,19);a.lineTo(12,19);a.lineTo(12,29);a.lineTo(6,29);a.lineTo(6,14);a.close();c.drawPath(a,p);}
        if(type==1){for(int a=0;a<2;a++)for(int b=0;b<2;b++)c.drawRoundRect(3+a*15,3+b*15,14+a*15,14+b*15,2,2,p);}
        if(type==2){p.setStyle(Paint.Style.STROKE);Path a=new Path();a.moveTo(4,12);a.lineTo(4,4);a.lineTo(29,4);a.lineTo(29,26);a.lineTo(20,26);c.drawPath(a,p);c.drawArc(-9,15,17,41,-90,90,false,p);c.drawArc(-4,20,12,36,-90,90,false,p);p.setStyle(Paint.Style.FILL);c.drawCircle(4,28,2,p);}
        if(type==3){p.setStyle(Paint.Style.STROKE);Path a=new Path();a.moveTo(7,7);a.lineTo(25,7);a.lineTo(30,15);a.lineTo(25,24);a.lineTo(7,24);a.lineTo(2,15);a.close();c.drawPath(a,p);}
        if(type==4){p.setStyle(Paint.Style.STROKE);c.drawCircle(16,16,10,p);c.drawCircle(16,16,3,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(16+(float)Math.cos(a)*10,16+(float)Math.sin(a)*10,16+(float)Math.cos(a)*14,16+(float)Math.sin(a)*14,p);}}
        p.setStyle(Paint.Style.FILL);c.restore();
    }
    class Tile extends View {
        App app;int index;boolean home;Paint p=new Paint(3);Drawable icon, banner;int color;GradientDrawable background;
        Tile(App a,int i,boolean h){super(MainActivity.this);app=a;index=i;home=h;setId(View.generateViewId());setFocusable(true);setClickable(true);setContentDescription(a.name);setOnClickListener(v->launch(app.pkg));
            color=colors[i % colors.length];
            icon=icons.get(app.pkg);if(icon==null)try{icon=getPackageManager().getApplicationIcon(app.pkg);icons.put(app.pkg,icon);}catch(Exception ignored){}
            banner=banners.get(app.pkg);
            if(banner==null)try{
                banner=getPackageManager().getApplicationBanner(app.pkg);
                if(banner==null){Intent intent=launchIntent(app.pkg);if(intent!=null&&intent.getComponent()!=null)banner=getPackageManager().getActivityInfo(intent.getComponent(),0).loadBanner(getPackageManager());}
                if(banner!=null)banners.put(app.pkg,banner);
            }catch(Exception ignored){}
            background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{color,0xff111418});background.setCornerRadius(x(15));
            setOnFocusChangeListener((v,f)->{if(f){if(home)lastHome=index;else lastLibrary=index;expand(false);}animate().cancel();animate().scaleX(f?1.025f:1).scaleY(f?1.025f:1).setDuration(reduced?0:110).start();invalidate();});
        }
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName(Button.class.getName());}
        @Override public boolean onKeyDown(int key,KeyEvent e){int cols=home?6:5;int dest=index;
            if(key==KeyEvent.KEYCODE_DPAD_LEFT){if(index%cols==0){nav[page].requestFocus();return true;}dest--;}
            else if(key==KeyEvent.KEYCODE_DPAD_RIGHT){if(index%cols!=cols-1&&index+1<tiles.size())dest++;}
            else if(key==KeyEvent.KEYCODE_DPAD_UP){if(index<cols){nav[page].requestFocus();return true;}dest-=cols;}
            else if(key==KeyEvent.KEYCODE_DPAD_DOWN){if(!home&&index+cols<tiles.size())dest+=cols;}
            else return super.onKeyDown(key,e);
            tiles.get(dest).requestFocus();return true;
        }
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=y(153);background.setBounds(x(5),y(5),w-x(5),h);background.draw(c);
            if(banner!=null){c.save();Path clip=new Path();clip.addRoundRect(new RectF(x(6),y(6),w-x(6),h-y(1)),x(14),y(14),Path.Direction.CW);c.clipPath(clip);banner.setBounds(x(6),y(6),w-x(6),h-y(1));banner.draw(c);c.restore();}
            else if(icon!=null){int size=y(home?90:82);int left=(w-size)/2;int top=(h-size)/2;icon.setBounds(left,top,left+size,top+size);icon.draw(c);}
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(x(hasFocus()?2.5f:1));p.setColor(hasFocus()?Color.WHITE:0x26ffffff);c.drawRoundRect(x(5),y(5),w-x(5),h,x(15),y(15),p);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(y(25));p.setColor(hasFocus()?Color.WHITE:0xffdddddd);String label=app.name;while(p.measureText(label)>w-x(18)&&label.length()>1)label=label.substring(0,label.length()-2)+"…";c.drawText(label,(w-p.measureText(label))/2,y(188),p);
        }
    }
}
