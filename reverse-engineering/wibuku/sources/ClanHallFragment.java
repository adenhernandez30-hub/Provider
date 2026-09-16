package wibuku.app.wibuku.ui.clan;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Editable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import defpackage.a24;
import defpackage.a70;
import defpackage.a9;
import defpackage.ae2;
import defpackage.b13;
import defpackage.b40;
import defpackage.b70;
import defpackage.c79;
import defpackage.er4;
import defpackage.es4;
import defpackage.f34;
import defpackage.f60;
import defpackage.f9;
import defpackage.fp1;
import defpackage.fw2;
import defpackage.gn2;
import defpackage.h64;
import defpackage.i70;
import defpackage.ii9;
import defpackage.iu;
import defpackage.iv;
import defpackage.je0;
import defpackage.jf7;
import defpackage.ju;
import defpackage.k51;
import defpackage.ke0;
import defpackage.kw3;
import defpackage.kx2;
import defpackage.l60;
import defpackage.l70;
import defpackage.le;
import defpackage.lh9;
import defpackage.lw3;
import defpackage.m70;
import defpackage.mg5;
import defpackage.mo1;
import defpackage.mr3;
import defpackage.n70;
import defpackage.nl2;
import defpackage.no2;
import defpackage.o03;
import defpackage.o60;
import defpackage.p70;
import defpackage.pm0;
import defpackage.po2;
import defpackage.q03;
import defpackage.q12;
import defpackage.q70;
import defpackage.qw3;
import defpackage.r03;
import defpackage.r60;
import defpackage.r70;
import defpackage.re0;
import defpackage.rk1;
import defpackage.rn2;
import defpackage.ro1;
import defpackage.rx0;
import defpackage.ry0;
import defpackage.s03;
import defpackage.s4;
import defpackage.t03;
import defpackage.t12;
import defpackage.t64;
import defpackage.tu3;
import defpackage.uk0;
import defpackage.uo1;
import defpackage.v03;
import defpackage.v13;
import defpackage.vg5;
import defpackage.vk3;
import defpackage.vm4;
import defpackage.xd2;
import defpackage.xr4;
import defpackage.yg2;
import defpackage.yi8;
import defpackage.ze1;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import org.json.JSONObject;
import wibuku.app.wibuku.R;
import wibuku.app.wibuku.model.anime.AnimeDetail;
import wibuku.app.wibuku.model.anime.AnimePosterTitlesKt;
import wibuku.app.wibuku.model.anime.Episode;
import wibuku.app.wibuku.model.anime.EpisodeMeta;
import wibuku.app.wibuku.model.anime.LoadedStreamSource;
import wibuku.app.wibuku.model.anime.StreamSource;
import wibuku.app.wibuku.model.pref.TogglePref;
import wibuku.app.wibuku.model.user.History;
import wibuku.app.wibuku.ui.clan.ClanHallFragment;

/* loaded from: classes3.dex */
public final class ClanHallFragment extends ro1 {
    public static final /* synthetic */ nl2[] T0;
    public final lh9 A0;
    public final re0 B0;
    public final re0 C0;
    public final re0 D0;
    public boolean E0;
    public vm4 F0;
    public boolean G0;
    public String H0;
    public boolean I0;
    public i70 J0;
    public float K0;
    public float L0;
    public boolean M0;
    public r60 N0;
    public long O0;
    public long P0;
    public vm4 Q0;
    public boolean R0;
    public final b70 S0;

    static {
        qw3 qw3Var = new qw3(ClanHallFragment.class, "binding", "getBinding()Lwibuku/app/wibuku/databinding/FragmentClanHallBinding;");
        a24.a.getClass();
        T0 = new nl2[]{qw3Var};
    }

    public ClanHallFragment() {
        super(R.layout.fragment_clan_hall);
        this.A0 = t12.n(this, a70.E);
        int i = 0;
        int i2 = 1;
        this.B0 = new re0(a24.a(fw2.class), new m70(this, i), new m70(this, 2), new m70(this, i2));
        iu iuVar = new iu(5, new m70(this, 3));
        rn2 rn2Var = rn2.y;
        gn2 d = jf7.d(rn2Var, iuVar);
        this.C0 = new re0(a24.a(r70.class), new ju(d, 10), new n70(this, d, i2), new ju(d, 11));
        gn2 d2 = jf7.d(rn2Var, new iu(6, new m70(this, 4)));
        this.D0 = new re0(a24.a(er4.class), new ju(d2, 12), new n70(this, d2, i), new ju(d2, 13));
        this.R0 = true;
        this.S0 = new b70(this);
    }

    public static Integer P0(Episode episode) {
        return es4.o0(xr4.M0((String) xr4.H0((CharSequence) xr4.H0((CharSequence) xr4.H0(episode.getName(), new String[]{"."}, 0, 6).get(0), new String[]{"-"}, 0, 6).get(0), new String[]{"_"}, 0, 6).get(0)).toString());
    }

    public static /* synthetic */ void e1(ClanHallFragment clanHallFragment, int i) {
        boolean z = true;
        if ((i & 1) != 0) {
            z = false;
        }
        clanHallFragment.d1(z, null);
    }

    public final void C0(String str, String str2, boolean z) {
        String obj = xr4.M0(str2).toString();
        if (xr4.y0(obj)) {
            return;
        }
        q70 q70Var = new q70(xr4.M0(str).toString(), obj, z);
        r70 M0 = M0();
        M0.getClass();
        ArrayList arrayList = M0.o;
        arrayList.add(q70Var);
        while (arrayList.size() > 80) {
            arrayList.remove(0);
        }
        D0(q70Var);
        while (L0().d.getChildCount() > M0().o.size()) {
            L0().d.removeViewAt(0);
        }
        L0().f.post(new o60(this, 1));
    }

    public final void D0(q70 q70Var) {
        int i;
        int i2;
        TextView textView = new TextView(w0());
        boolean z = q70Var.c;
        String str = q70Var.b;
        String str2 = q70Var.a;
        if (!z && !xr4.y0(str2)) {
            str = mo1.k(str2, ": ", str);
        }
        textView.setText(str);
        if (z) {
            i = R.color.white2;
        } else {
            i = R.color.white;
        }
        textView.setTextColor(w0().getColor(i));
        textView.setTextSize(12.0f);
        Context w0 = w0();
        if (z) {
            i2 = R.font.roboto;
        } else {
            i2 = R.font.montserrat_bold;
        }
        textView.setTypeface(h64.a(w0, i2));
        textView.setPadding(0, J0(4), 0, J0(4));
        L0().d.addView(textView);
    }

    public final void E0(boolean z, boolean z2) {
        int i;
        float f;
        int i2;
        int i3;
        M0().g = z;
        i70 i70Var = this.J0;
        if (i70Var != null) {
            i70Var.e(z);
        }
        String str = null;
        boolean z3 = true;
        int i4 = 0;
        if (z2) {
            if (z) {
                uo1 M = M();
                if (M != null) {
                    if (M0().h == null) {
                        M0().h = Integer.valueOf(M.getRequestedOrientation());
                    }
                    M.setRequestedOrientation(6);
                }
            } else {
                M0().n = false;
                uo1 M2 = M();
                if (M2 != null) {
                    Integer num = M0().h;
                    if (num != null) {
                        i3 = num.intValue();
                    } else {
                        i3 = 1;
                    }
                    M0().h = null;
                    M2.setRequestedOrientation(i3);
                }
            }
        }
        WebView webView = L0().l;
        if (!z) {
            i = 0;
        } else {
            i = 8;
        }
        webView.setVisibility(i);
        FrameLayout frameLayout = L0().k;
        if (z) {
            f = 32.0f;
        } else {
            f = 0.0f;
        }
        frameLayout.setElevation(f);
        ViewGroup.LayoutParams layoutParams = L0().k.getLayoutParams();
        layoutParams.getClass();
        uk0 uk0Var = (uk0) layoutParams;
        if (!z) {
            str = "16:9";
        }
        uk0Var.G = str;
        uk0Var.i = 0;
        if (z) {
            i2 = 0;
        } else {
            i2 = -1;
        }
        uk0Var.l = i2;
        ((ViewGroup.MarginLayoutParams) uk0Var).width = 0;
        ((ViewGroup.MarginLayoutParams) uk0Var).height = 0;
        L0().k.setLayoutParams(uk0Var);
        if (L0().k.getVisibility() != 0) {
            z3 = false;
        }
        i1(z3);
        l1();
        j1(M0().f);
        View findViewById = L0().j.findViewById(R.id.anime_expand);
        if (findViewById != null) {
            if (!z) {
                i4 = 8;
            }
            findViewById.setVisibility(i4);
        }
        U0();
        h1();
        L0().j.i();
    }

    public final void F0(f34 f34Var) {
        float f;
        SubtitleView subtitleView = L0().j.getSubtitleView();
        if (subtitleView == null) {
            return;
        }
        if (M0().g) {
            f = 34.0f;
        } else {
            f = 24.0f;
        }
        q12.F(subtitleView, f34Var, f);
    }

    public final void G0(boolean z, long j) {
        ze1 ze1Var = O0().K;
        if (ze1Var != null) {
            this.I0 = true;
            int i = 4;
            try {
                if (ze1Var.q() > 0) {
                    if (j < 0) {
                        j = 0;
                    }
                    ze1Var.K(j);
                }
                ze1Var.Q(z);
                View view = this.e0;
                if (view != null) {
                    view.postDelayed(new o60(this, i), 300L);
                }
            } catch (Throwable th) {
                View view2 = this.e0;
                if (view2 != null) {
                    view2.postDelayed(new o60(this, i), 300L);
                }
                throw th;
            }
        }
    }

    public final void H0(long j, String str) {
        if (this.e0 != null) {
            if (j < 0) {
                j = 0;
            }
            long currentTimeMillis = System.currentTimeMillis();
            if (str.equals("watch_seek") && j == M0().i && currentTimeMillis - M0().j < 500) {
                return;
            }
            M0().i = j;
            M0().j = currentTimeMillis;
            L0().l.evaluateJavascript("window.WIBUKU_NATIVE_WATCH_CONTROL && window.WIBUKU_NATIVE_WATCH_CONTROL('" + str + "'," + j + ");", null);
        }
    }

    public final void I0(boolean z) {
        if (!z && L0().c.hasFocus()) {
            L0().c.clearFocus();
            L0().c.setCursorVisible(false);
        }
    }

    public final int J0(int i) {
        return (int) (i * S().getDisplayMetrics().density);
    }

    public final long K0() {
        long j = M0().k;
        long j2 = M0().l;
        if (M0().m && j2 > 0) {
            long currentTimeMillis = (System.currentTimeMillis() - j2) + j;
            if (currentTimeMillis < 0) {
                return 0L;
            }
            return currentTimeMillis;
        }
        return j;
    }

    public final fp1 L0() {
        return (fp1) this.A0.q(this, T0[0]);
    }

    public final r70 M0() {
        return (r70) this.C0.getValue();
    }

    public final fw2 N0() {
        return (fw2) this.B0.getValue();
    }

    public final er4 O0() {
        return (er4) this.D0.getValue();
    }

    public final List Q0(AnimeDetail animeDetail) {
        List<Episode> episodes = animeDetail.getEpisodes();
        if (episodes == null || !episodes.isEmpty()) {
            Iterator<T> it = episodes.iterator();
            while (it.hasNext()) {
                if (P0((Episode) it.next()) != null) {
                    return je0.t0(episodes, new l60(2, new l60(1, new v13(19, this))));
                }
            }
        }
        episodes.getClass();
        return new kx2(episodes);
    }

    /* JADX WARN: Code restructure failed: missing block: B:11:0x0025, code lost:
    
        if (r0 != 3) goto L38;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final boolean R0(android.view.MotionEvent r9) {
        /*
            Method dump skipped, instructions count: 296
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: wibuku.app.wibuku.ui.clan.ClanHallFragment.R0(android.view.MotionEvent):boolean");
    }

    public final void S0(EpisodeMeta episodeMeta) {
        if (episodeMeta == null) {
            return;
        }
        LoadedStreamSource loadedStreamSource = (LoadedStreamSource) O0().v.d();
        if (loadedStreamSource != null && !xr4.y0(loadedStreamSource.getDirect()) && !yg2.a(loadedStreamSource.getDirect(), "errorcuy")) {
            List<StreamSource> stream_sources = episodeMeta.getStream_sources();
            ArrayList arrayList = new ArrayList(ke0.Q(stream_sources, 10));
            Iterator<T> it = stream_sources.iterator();
            while (it.hasNext()) {
                arrayList.add(Long.valueOf(((StreamSource) it.next()).getId()));
            }
            if (arrayList.contains(Long.valueOf(loadedStreamSource.getStreamSource().getId()))) {
                T0(loadedStreamSource);
                return;
            }
        }
        O0().n(yi8.p(U()), N0(), episodeMeta, false);
    }

    /* JADX WARN: Type inference failed for: r14v0, types: [p03, o03] */
    public final void T0(LoadedStreamSource loadedStreamSource) {
        if (loadedStreamSource != null) {
            if (!xr4.y0(loadedStreamSource.getDirect()) && !yg2.a(loadedStreamSource.getDirect(), "errorcuy")) {
                ze1 ze1Var = O0().K;
                if (ze1Var != null) {
                    Uri parse = Uri.parse(loadedStreamSource.getDirect());
                    parse.getClass();
                    int i = v03.g;
                    rk1 rk1Var = new rk1();
                    xd2 xd2Var = ae2.y;
                    f34 f34Var = f34.B;
                    List list = Collections.EMPTY_LIST;
                    xd2 xd2Var2 = ae2.y;
                    f34 f34Var2 = f34.B;
                    q03 q03Var = new q03();
                    v03 v03Var = new v03("", new o03(rk1Var), new s03(parse, null, null, list, f34Var2, -9223372036854775807L), new r03(q03Var), b13.C, t03.a);
                    vk3 vk3Var = M0().d;
                    if (vk3Var != null) {
                        k1(AnimePosterTitlesKt.displayTitles((AnimeDetail) vk3Var.x).getMain(), ((Episode) vk3Var.y).getName());
                    }
                    TextView textView = (TextView) L0().j.findViewById(R.id.playback_quality);
                    if (textView != null) {
                        textView.setText(loadedStreamSource.getStreamSource().getQuality().getTag());
                    }
                    kw3 kw3Var = O0().M;
                    if (kw3Var != null) {
                        lw3 d = kw3Var.d(v03Var);
                        ze1Var.a0();
                        ze1Var.O(Collections.singletonList(d), false);
                        ze1Var.F();
                        long j = M0().k;
                        if (j < 0) {
                            j = 0;
                        }
                        G0(M0().m, j);
                        L0().k.setVisibility(0);
                        int i2 = 1;
                        i1(true);
                        vm4 vm4Var = this.F0;
                        if (vm4Var == null || !vm4Var.a()) {
                            this.F0 = ii9.k(yi8.p(U()), null, new l70(this, null, i2), 3);
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            iv.l(-1, "Stream nobar belum siap");
        }
    }

    public final void U0() {
        int i;
        PlayerView playerView = L0().j;
        if (M0().g && ((TogglePref) mr3.M.getValue()).get()) {
            i = 3;
        } else {
            i = 0;
        }
        playerView.setResizeMode(i);
        L0().j.requestLayout();
    }

    public final void V0(long j) {
        ze1 ze1Var;
        long j2;
        if (!M0().f || (ze1Var = O0().K) == null || ze1Var.q() <= 0) {
            return;
        }
        long p = ze1Var.p();
        Long valueOf = Long.valueOf(p);
        pm0 pm0Var = null;
        if (p <= 0) {
            valueOf = null;
        }
        if (valueOf != null) {
            j2 = valueOf.longValue();
        } else {
            j2 = Long.MAX_VALUE;
        }
        long j3 = tu3.j(ze1Var.k() + j, 0L, j2);
        ze1Var.K(j3);
        H0(j3, "watch_seek");
        L0().j.i();
        TextView textView = (TextView) L0().j.findViewById(R.id.replay_text);
        TextView textView2 = (TextView) L0().j.findViewById(R.id.forward_text);
        if (textView != null) {
            textView.setVisibility(4);
        }
        if (textView2 != null) {
            textView2.setVisibility(4);
        }
        int i = 0;
        if (j > 0) {
            this.O0 += j;
            this.P0 = 0L;
            if (textView2 != null) {
                textView2.setVisibility(0);
                f9.a(textView2);
                textView2.setText("+" + (this.O0 / 1000) + "s");
            }
        } else {
            this.P0 = Math.abs(j) + this.P0;
            this.O0 = 0L;
            if (textView != null) {
                textView.setVisibility(0);
                f9.b(textView);
                textView.setText("-" + (this.P0 / 1000) + "s");
            }
        }
        vm4 vm4Var = this.Q0;
        if (vm4Var != null) {
            c79.B(vm4Var);
        }
        this.Q0 = ii9.k(yi8.p(U()), null, new l70(this, pm0Var, i), 3);
    }

    public final void W0() {
        String str;
        String obj;
        Editable text = L0().c.getText();
        if (text != null && (obj = text.toString()) != null) {
            str = xr4.M0(obj).toString();
        } else {
            str = null;
        }
        if (str == null) {
            str = "";
        }
        if (xr4.y0(str)) {
            return;
        }
        L0().c.setText("");
        L0().c.clearFocus();
        L0().c.setCursorVisible(false);
        L0().l.evaluateJavascript(mo1.k("(function(){return window.WIBUKU_NATIVE_SEND_CHAT ? window.WIBUKU_NATIVE_SEND_CHAT(", JSONObject.quote(str), ") : 'missing';})()"), new ValueCallback() { // from class: u60
            @Override // android.webkit.ValueCallback
            public final void onReceiveValue(Object obj2) {
                String str2 = (String) obj2;
                nl2[] nl2VarArr = ClanHallFragment.T0;
                ClanHallFragment clanHallFragment = ClanHallFragment.this;
                if (clanHallFragment.e0 != null && yg2.a(str2, "\"missing\"")) {
                    clanHallFragment.C0("", "Chat belum siap, coba lagi sebentar.", true);
                }
            }
        });
    }

    public final void X0(String str) {
        this.H0 = str;
        if (!this.G0 || str == null) {
            return;
        }
        L0().l.evaluateJavascript("window.WIBUKU_HALL_WATCH_SELECTED && window.WIBUKU_HALL_WATCH_SELECTED(" + str + ");", null);
        this.H0 = null;
    }

    public final void Y0(boolean z) {
        i70 i70Var;
        if (M0().g == z && (i70Var = this.J0) != null && i70Var.b == z) {
            return;
        }
        E0(z, true);
    }

    public final void Z0(String str) {
        M0().c = str;
    }

    public final void a1() {
        uo1 M;
        Object t64Var;
        boolean z;
        if (O0().d > 0 && (M = M()) != null && X() && q12.e(M)) {
            try {
                if (this.n0.h.compareTo(no2.A) >= 0) {
                    z = true;
                } else {
                    z = false;
                }
                t64Var = Boolean.valueOf(z);
            } catch (Throwable th) {
                t64Var = new t64(th);
            }
            Object obj = Boolean.FALSE;
            if (t64Var instanceof t64) {
                t64Var = obj;
            }
            if (((Boolean) t64Var).booleanValue()) {
                long j = O0().d;
                int i = O0().k;
                Long l = O0().e;
                Long l2 = O0().f;
                long j2 = O0().g;
                long j3 = O0().h;
                long j4 = O0().i;
                int i2 = O0().j;
                long j5 = O0().l;
                O0().d = 0L;
                O0().k = 0;
                O0().l = 0L;
                O0().e = null;
                O0().f = null;
                O0().g = 0L;
                O0().h = 0L;
                O0().i = 0L;
                O0().j = 0;
                q12.w(j, i, l, l2, M, Long.valueOf(j2), Long.valueOf(j3), Long.valueOf(j4), i2, j5, 1024);
            }
        }
    }

    public final void b1(String str) {
        View view;
        if (!X() || (view = this.e0) == null) {
            return;
        }
        fp1 a = fp1.a(view);
        LinearLayout linearLayout = a.h;
        linearLayout.animate().cancel();
        linearLayout.setAlpha(1.0f);
        linearLayout.setVisibility(0);
        a.i.setText(str);
    }

    public final void c1(int i) {
        vk3 vk3Var;
        String str;
        if (M0().f && (vk3Var = M0().d) != null) {
            AnimeDetail animeDetail = (AnimeDetail) vk3Var.x;
            Episode episode = (Episode) vk3Var.y;
            List Q0 = Q0(animeDetail);
            Iterator it = Q0.iterator();
            int i2 = 0;
            while (true) {
                if (it.hasNext()) {
                    if (((Episode) it.next()).getId() == episode.getId()) {
                        break;
                    } else {
                        i2++;
                    }
                } else {
                    i2 = -1;
                    break;
                }
            }
            if (i2 < 0) {
                return;
            }
            Episode episode2 = (Episode) je0.d0(i2 + i, Q0);
            if (episode2 == null) {
                if (i > 0) {
                    str = "Episode berikutnya tidak ada";
                } else {
                    str = "Episode sebelumnya tidak ada";
                }
                iv.l(-1, str);
                return;
            }
            N0();
            fw2.s(this, animeDetail, episode2, new f60(1, this, animeDetail, episode2), new s4(5));
        }
    }

    public final void d1(boolean z, le leVar) {
        EpisodeMeta episodeMeta = (EpisodeMeta) O0().t.d();
        vk3 vk3Var = M0().d;
        if (episodeMeta != null && vk3Var != null) {
            long j = M0().e;
            if (j <= 0) {
                if (z) {
                    a1();
                }
                if (leVar != null) {
                    leVar.b();
                    return;
                }
                return;
            }
            M0().e = 0L;
            History history = episodeMeta.getHistory();
            ze1 ze1Var = O0().K;
            if (ze1Var != null) {
                long p = ze1Var.p();
                long j2 = 10800000;
                if (p > 10800000) {
                    p = 10800000;
                }
                if (p < 0) {
                    p = 0;
                }
                long k = ze1Var.k();
                if (k <= 10800000) {
                    j2 = k;
                }
                if (j2 < 0) {
                    j2 = 0;
                }
                history.setProgress(j2);
                if (history.getMaxProgress() <= 0 || p > 0) {
                    history.setMaxProgress(p);
                }
                history.setCreatedAt(new Date());
            }
            ((TogglePref) mr3.C.getValue()).toggle(Boolean.TRUE);
            N0().b.i(history);
            N0().n((AnimeDetail) vk3Var.x, ((Episode) vk3Var.y).getName(), history.getProgress(), history.getMaxProgress());
            po2 p2 = yi8.p(this);
            ry0 ry0Var = k51.a;
            ii9.k(p2, rx0.z, new p70(history, j, this, z, leVar, null), 2);
            return;
        }
        if (z) {
            a1();
        }
        if (leVar != null) {
            leVar.b();
        }
    }

    public final void f1() {
        AnimeDetail animeDetail;
        int i;
        boolean z;
        int i2;
        int i3;
        int i4;
        vk3 vk3Var = M0().d;
        Episode episode = null;
        if (vk3Var != null) {
            animeDetail = (AnimeDetail) vk3Var.x;
        } else {
            animeDetail = null;
        }
        if (vk3Var != null) {
            episode = (Episode) vk3Var.y;
        }
        int i5 = -1;
        int i6 = 0;
        if (animeDetail != null && episode != null) {
            Iterator it = Q0(animeDetail).iterator();
            i = 0;
            while (it.hasNext()) {
                if (((Episode) it.next()).getId() == episode.getId()) {
                    break;
                } else {
                    i++;
                }
            }
        }
        i = -1;
        boolean z2 = true;
        if (M0().f && i > 0) {
            z = true;
        } else {
            z = false;
        }
        if (animeDetail != null) {
            i5 = Q0(animeDetail).size() - 1;
        }
        if (!M0().f || i < 0 || i >= i5) {
            z2 = false;
        }
        View findViewById = L0().j.findViewById(R.id.prev_episode);
        if (findViewById != null) {
            if (z) {
                i4 = 0;
            } else {
                i4 = 8;
            }
            findViewById.setVisibility(i4);
        }
        View findViewById2 = L0().j.findViewById(R.id.prev_episode_text);
        if (findViewById2 != null) {
            if (z) {
                i3 = 0;
            } else {
                i3 = 8;
            }
            findViewById2.setVisibility(i3);
        }
        View findViewById3 = L0().j.findViewById(R.id.next_episode);
        if (findViewById3 != null) {
            if (z2) {
                i2 = 0;
            } else {
                i2 = 8;
            }
            findViewById3.setVisibility(i2);
        }
        View findViewById4 = L0().j.findViewById(R.id.next_episode_text);
        if (findViewById4 != null) {
            if (!z2) {
                i6 = 8;
            }
            findViewById4.setVisibility(i6);
        }
    }

    @Override // defpackage.ro1
    public final void g0() {
        boolean z;
        int i;
        uo1 M;
        Window window;
        View decorView;
        ViewTreeObserver viewTreeObserver;
        uo1 M2 = M();
        if (M2 != null && M2.isChangingConfigurations()) {
            z = true;
        } else {
            z = false;
        }
        WebView webView = L0().l;
        r60 r60Var = this.N0;
        if (r60Var != null && (M = M()) != null && (window = M.getWindow()) != null && (decorView = window.getDecorView()) != null && (viewTreeObserver = decorView.getViewTreeObserver()) != null) {
            viewTreeObserver.removeOnGlobalLayoutListener(r60Var);
        }
        this.N0 = null;
        ConstraintLayout constraintLayout = L0().a;
        WeakHashMap weakHashMap = vg5.a;
        mg5.c(constraintLayout, null);
        L0().h.animate().cancel();
        if (!z) {
            try {
                if (!this.E0) {
                    this.E0 = true;
                    try {
                        L0().l.evaluateJavascript("window.WIBUKU_LEAVE_HALL && window.WIBUKU_LEAVE_HALL();", null);
                    } catch (Throwable unused) {
                    }
                }
            } catch (Throwable unused2) {
            }
        }
        webView.postDelayed(new a9(11, this, webView), 180L);
        e1(this, 3);
        vm4 vm4Var = this.F0;
        if (vm4Var != null) {
            vm4Var.g(null);
        }
        vm4 vm4Var2 = this.Q0;
        if (vm4Var2 != null) {
            c79.B(vm4Var2);
        }
        this.Q0 = null;
        if (!z) {
            Y0(false);
            uo1 M3 = M();
            if (M3 != null) {
                Integer num = M0().h;
                if (num != null) {
                    i = num.intValue();
                } else {
                    i = 1;
                }
                M0().h = null;
                M3.setRequestedOrientation(i);
            }
        }
        ze1 ze1Var = O0().K;
        if (ze1Var != null) {
            ze1Var.H(this.S0);
        }
        L0().j.setPlayer(null);
        this.c0 = true;
    }

    public final void g1(int i) {
        FrameLayout.LayoutParams layoutParams;
        int J0 = J0(96);
        int i2 = 0;
        if (M0().g) {
            if (i < 0) {
                i = 0;
            }
            i2 = i;
        }
        int i3 = J0 + i2;
        ViewGroup.LayoutParams layoutParams2 = L0().e.getLayoutParams();
        if (layoutParams2 instanceof FrameLayout.LayoutParams) {
            layoutParams = (FrameLayout.LayoutParams) layoutParams2;
        } else {
            layoutParams = null;
        }
        if (layoutParams != null && layoutParams.bottomMargin != i3) {
            layoutParams.bottomMargin = i3;
            L0().e.setLayoutParams(layoutParams);
        }
    }

    public final void h1() {
        boolean z;
        int i;
        String str;
        int i2 = 0;
        if (M0().g && M0().n) {
            z = true;
        } else {
            z = false;
        }
        LinearLayout linearLayout = L0().e;
        if (z) {
            i = 0;
        } else {
            i = 8;
        }
        linearLayout.setVisibility(i);
        TextView textView = (TextView) L0().j.findViewById(R.id.hall_live_chat_toggle);
        if (textView != null) {
            if (!M0().g) {
                i2 = 8;
            }
            textView.setVisibility(i2);
            if (z) {
                str = "Chat On";
            } else {
                str = "Chat";
            }
            textView.setText(str);
        }
    }

    public final void i1(boolean z) {
        boolean z2;
        if (this.e0 != null && this.G0) {
            if (z && !M0().g) {
                z2 = true;
            } else {
                z2 = false;
            }
            L0().l.evaluateJavascript("window.WIBUKU_NATIVE_PLAYER_INSET && window.WIBUKU_NATIVE_PLAYER_INSET(" + z2 + ");", null);
        }
    }

    public final void j1(boolean z) {
        b40 b40Var;
        int i;
        int i2;
        L0().j.setUseController(true);
        L0().j.setClickable(true);
        L0().j.setFocusable(true);
        View findViewById = L0().j.findViewById(R.id.exo_play_pause);
        int i3 = 8;
        if (findViewById != null) {
            if (z) {
                i2 = 0;
            } else {
                i2 = 8;
            }
            findViewById.setVisibility(i2);
        }
        View findViewById2 = L0().j.findViewById(R.id.replay_episode);
        if (findViewById2 != null) {
            if (z) {
                i = 0;
            } else {
                i = 8;
            }
            findViewById2.setVisibility(i);
        }
        View findViewById3 = L0().j.findViewById(R.id.forward_episode);
        if (findViewById3 != null) {
            if (z) {
                i3 = 0;
            }
            findViewById3.setVisibility(i3);
        }
        View findViewById4 = L0().j.findViewById(R.id.replay_text);
        if (findViewById4 != null) {
            findViewById4.setVisibility(4);
        }
        View findViewById5 = L0().j.findViewById(R.id.forward_text);
        if (findViewById5 != null) {
            findViewById5.setVisibility(4);
        }
        View findViewById6 = L0().j.findViewById(R.id.exo_progress);
        if (findViewById6 != null) {
            findViewById6.setEnabled(z);
            findViewById6.setClickable(!z);
            if (z) {
                b40Var = null;
            } else {
                b40Var = new b40(1);
            }
            findViewById6.setOnTouchListener(b40Var);
        }
        f1();
        if (!z) {
            L0().j.i();
        } else {
            L0().j.i();
        }
    }

    @Override // defpackage.ro1
    public final void k0() {
        boolean z;
        ze1 ze1Var;
        uo1 M = M();
        if (M != null && M.isChangingConfigurations()) {
            z = true;
        } else {
            z = false;
        }
        if (!z && !this.E0) {
            this.E0 = true;
            try {
                L0().l.evaluateJavascript("window.WIBUKU_LEAVE_HALL && window.WIBUKU_LEAVE_HALL();", null);
            } catch (Throwable unused) {
            }
        }
        this.c0 = true;
        e1(this, 3);
        if (!z && (ze1Var = O0().K) != null) {
            ze1Var.Q(false);
        }
        L0().l.onPause();
    }

    public final void k1(String str, String str2) {
        String str3;
        String str4;
        int i;
        TextView textView = (TextView) L0().j.findViewById(R.id.anime_title_full);
        int i2 = 8;
        String str5 = "";
        if (textView != null) {
            if (str == null) {
                str4 = "";
            } else {
                str4 = str;
            }
            textView.setText(str4);
            if (str != null && !xr4.y0(str)) {
                i = 0;
            } else {
                i = 8;
            }
            textView.setVisibility(i);
        }
        TextView textView2 = (TextView) L0().j.findViewById(R.id.anime_episode_full);
        if (textView2 != null) {
            if (str2 != null) {
                str3 = xr4.M0(str2).toString();
            } else {
                str3 = null;
            }
            if (str3 == null) {
                str3 = "";
            }
            if (!xr4.y0(str3)) {
                if (es4.n0(str3, "Episode", true)) {
                    str5 = str3;
                } else {
                    str5 = "Episode ".concat(str3);
                }
            }
            textView2.setText(str5);
            if (str2 != null && !xr4.y0(str2)) {
                i2 = 0;
            }
            textView2.setVisibility(i2);
        }
    }

    @Override // defpackage.ro1
    public final void l0() {
        boolean z = true;
        this.c0 = true;
        this.E0 = false;
        L0().l.onResume();
        L0().l.evaluateJavascript("window.WIBUKU_RESUME_HALL && window.WIBUKU_RESUME_HALL();", null);
        L0().l.evaluateJavascript("window.WIBUKU_REAPPLY_WATCH && window.WIBUKU_REAPPLY_WATCH();", null);
        if (L0().k.getVisibility() != 0) {
            z = false;
        }
        i1(z);
    }

    public final void l1() {
        float f;
        Resources resources;
        SubtitleView subtitleView = L0().j.getSubtitleView();
        if (subtitleView != null) {
            if (M0().g) {
                f = 23.0f;
            } else {
                f = 15.0f;
            }
            Context context = subtitleView.getContext();
            if (context == null) {
                resources = Resources.getSystem();
            } else {
                resources = context.getResources();
            }
            float applyDimension = TypedValue.applyDimension(2, f, resources.getDisplayMetrics());
            subtitleView.z = 2;
            subtitleView.A = applyDimension;
            subtitleView.c();
            subtitleView.setBottomPaddingFraction(0.08f);
        }
    }

    @Override // defpackage.ro1, android.content.ComponentCallbacks
    public final void onConfigurationChanged(Configuration configuration) {
        configuration.getClass();
        this.c0 = true;
        if (X() && M0().g) {
            L0().a.post(new o60(this, 0));
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:101:0x04c5, code lost:
    
        if (r0 != null) goto L107;
     */
    /* JADX WARN: Code restructure failed: missing block: B:117:0x04ed, code lost:
    
        if (r2 > 0) goto L119;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:105:0x0516  */
    /* JADX WARN: Removed duplicated region for block: B:108:0x051e  */
    /* JADX WARN: Type inference failed for: r0v72, types: [r60] */
    /* JADX WARN: Type inference failed for: r2v99, types: [android.view.View$OnFocusChangeListener, java.lang.Object] */
    /* JADX WARN: Type inference failed for: r4v15, types: [v14, java.lang.Object] */
    /* JADX WARN: Type inference failed for: r5v17, types: [v14, java.lang.Object] */
    @Override // defpackage.ro1
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void p0(android.view.View r19) {
        /*
            Method dump skipped, instructions count: 1340
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: wibuku.app.wibuku.ui.clan.ClanHallFragment.p0(android.view.View):void");
    }
}
