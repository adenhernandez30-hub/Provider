package wibuku.app.wibuku.model.anime;

import defpackage.aj3;
import defpackage.cv0;
import defpackage.es4;
import defpackage.ex1;
import defpackage.fb5;
import defpackage.gv;
import defpackage.hw8;
import defpackage.i04;
import defpackage.k64;
import defpackage.nh;
import defpackage.nu4;
import defpackage.og2;
import defpackage.pm0;
import defpackage.tn1;
import defpackage.un0;
import defpackage.un1;
import defpackage.v44;
import defpackage.vn0;
import defpackage.xr4;
import defpackage.yi3;

@cv0(c = "wibuku.app.wibuku.model.anime.StreamSource$getDirectLink$3", f = "StreamSource.kt", l = {92}, m = "invokeSuspend", v = 2)
/* loaded from: classes.dex */
public final class StreamSource$getDirectLink$3 extends nu4 implements ex1 {
    final /* synthetic */ String $hash;
    final /* synthetic */ String $newurl;
    final /* synthetic */ String $token;
    int label;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public StreamSource$getDirectLink$3(String str, String str2, String str3, pm0<? super StreamSource$getDirectLink$3> pm0Var) {
        super(2, pm0Var);
        this.$newurl = str;
        this.$hash = str2;
        this.$token = str3;
    }

    private static final fb5 invokeSuspend$lambda$0(String str, String str2, String str3, v44 v44Var) {
        v44Var.c(str);
        tn1 tn1Var = new tn1();
        invokeSuspend$lambda$0$0(v44Var, str2, str3, tn1Var);
        v44Var.b("POST", new un1(tn1Var.a, tn1Var.b));
        return fb5.a;
    }

    private static final fb5 invokeSuspend$lambda$0$0(v44 v44Var, String str, String str2, tn1 tn1Var) {
        v44Var.getClass();
        v44Var.c.a("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");
        str.getClass();
        v44Var.c.a("hash", str);
        tn1Var.a("token", str2);
        return fb5.a;
    }

    @Override // defpackage.lo
    public final pm0<fb5> create(Object obj, pm0<?> pm0Var) {
        return new StreamSource$getDirectLink$3(this.$newurl, this.$hash, this.$token, pm0Var);
    }

    @Override // defpackage.ex1
    public final Object invoke(un0 un0Var, pm0<? super String> pm0Var) {
        return ((StreamSource$getDirectLink$3) create(un0Var, pm0Var)).invokeSuspend(fb5.a);
    }

    @Override // defpackage.lo
    public final Object invokeSuspend(Object obj) {
        int i = this.label;
        try {
            if (i != 0) {
                if (i == 1) {
                    og2.l(obj);
                } else {
                    nh.l("call to 'resume' before 'invoke' with coroutine");
                    return null;
                }
            } else {
                og2.l(obj);
                yi3 yi3Var = (yi3) aj3.d.getValue();
                String str = this.$newurl;
                String str2 = this.$hash;
                String str3 = this.$token;
                v44 v44Var = new v44();
                v44Var.a("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");
                invokeSuspend$lambda$0(str, str2, str3, v44Var);
                hw8 hw8Var = new hw8(v44Var);
                yi3Var.getClass();
                i04 i04Var = new i04(yi3Var, hw8Var);
                this.label = 1;
                obj = gv.a(i04Var, this);
                vn0 vn0Var = vn0.x;
                if (obj == vn0Var) {
                    return vn0Var;
                }
            }
            String p = ((k64) obj).D.p();
            if (p == null) {
                p = "";
            }
            return es4.l0((String) xr4.H0((CharSequence) xr4.H0(p, new String[]{"url\":\""}, 0, 6).get(1), new String[]{"\""}, 0, 6).get(0), "\\/", "/");
        } catch (Exception e) {
            new Integer(1);
            e.getMessage();
            return "";
        }
    }
}
