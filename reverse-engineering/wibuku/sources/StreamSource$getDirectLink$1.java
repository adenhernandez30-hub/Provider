package wibuku.app.wibuku.model.anime;

import defpackage.cv0;
import defpackage.pm0;
import defpackage.rm0;

@cv0(c = "wibuku.app.wibuku.model.anime.StreamSource", f = "StreamSource.kt", l = {34, 53, 69, 73, 83, 116}, m = "getDirectLink", v = 2)
/* loaded from: classes.dex */
public final class StreamSource$getDirectLink$1 extends rm0 {
    Object L$0;
    Object L$1;
    Object L$2;
    Object L$3;
    Object L$4;
    Object L$5;
    Object L$6;
    int label;
    /* synthetic */ Object result;
    final /* synthetic */ StreamSource this$0;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public StreamSource$getDirectLink$1(StreamSource streamSource, pm0<? super StreamSource$getDirectLink$1> pm0Var) {
        super(pm0Var);
        this.this$0 = streamSource;
    }

    @Override // defpackage.lo
    public final Object invokeSuspend(Object obj) {
        this.result = obj;
        this.label |= Integer.MIN_VALUE;
        return this.this$0.getDirectLink(this);
    }
}
