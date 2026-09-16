package wibuku.app.wibuku.model.anime;

import defpackage.mk0;
import defpackage.po4;
import defpackage.qd4;
import defpackage.yg2;

/* loaded from: classes.dex */
public final class StreamSource {

    @qd4("id")
    private final long id;

    @qd4("link")
    private final String link;

    @qd4("quality")
    private final StreamQuality quality;

    @qd4("type")
    private final String type;

    public StreamSource(long j, String str, String str2, StreamQuality streamQuality) {
        str.getClass();
        str2.getClass();
        streamQuality.getClass();
        this.id = j;
        this.type = str;
        this.link = str2;
        this.quality = streamQuality;
    }

    public static /* synthetic */ StreamSource copy$default(StreamSource streamSource, long j, String str, String str2, StreamQuality streamQuality, int i, Object obj) {
        if ((i & 1) != 0) {
            j = streamSource.id;
        }
        long j2 = j;
        if ((i & 2) != 0) {
            str = streamSource.type;
        }
        String str3 = str;
        if ((i & 4) != 0) {
            str2 = streamSource.link;
        }
        String str4 = str2;
        if ((i & 8) != 0) {
            streamQuality = streamSource.quality;
        }
        return streamSource.copy(j2, str3, str4, streamQuality);
    }

    public final long component1() {
        return this.id;
    }

    public final String component2() {
        return this.type;
    }

    public final String component3() {
        return this.link;
    }

    public final StreamQuality component4() {
        return this.quality;
    }

    public final StreamSource copy(long j, String str, String str2, StreamQuality streamQuality) {
        str.getClass();
        str2.getClass();
        streamQuality.getClass();
        return new StreamSource(j, str, str2, streamQuality);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StreamSource)) {
            return false;
        }
        StreamSource streamSource = (StreamSource) obj;
        return this.id == streamSource.id && yg2.a(this.type, streamSource.type) && yg2.a(this.link, streamSource.link) && this.quality == streamSource.quality;
    }

    /* JADX WARN: Can't wrap try/catch for region: R(7:1|(2:3|(4:5|6|7|8))|127|6|7|8|(1:(0))) */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x00ff, code lost:
    
        if (r0 == r15) goto L100;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x0188, code lost:
    
        if (r0 == r15) goto L100;
     */
    /* JADX WARN: Code restructure failed: missing block: B:121:0x024f, code lost:
    
        if (r0 == r15) goto L100;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x0344, code lost:
    
        if (r0 == r15) goto L100;
     */
    /* JADX WARN: Failed to find 'out' block for switch in B:8:0x0031. Please report as an issue. */
    /* JADX WARN: Removed duplicated region for block: B:12:0x003a  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0050  */
    /* JADX WARN: Removed duplicated region for block: B:26:0x0073  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x007c  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x0089  */
    /* JADX WARN: Removed duplicated region for block: B:78:0x009a  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x00a2  */
    /* JADX WARN: Removed duplicated region for block: B:9:0x0034  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final java.lang.Object getDirectLink(defpackage.pm0<? super java.lang.String> r17) {
        /*
            Method dump skipped, instructions count: 1092
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: wibuku.app.wibuku.model.anime.StreamSource.getDirectLink(pm0):java.lang.Object");
    }

    public final long getId() {
        return this.id;
    }

    public final String getLink() {
        return this.link;
    }

    public final StreamQuality getQuality() {
        return this.quality;
    }

    public final String getType() {
        return this.type;
    }

    public int hashCode() {
        return this.quality.hashCode() + mk0.f(this.link, mk0.f(this.type, Long.hashCode(this.id) * 31, 31), 31);
    }

    public String toString() {
        long j = this.id;
        String str = this.type;
        String str2 = this.link;
        StreamQuality streamQuality = this.quality;
        StringBuilder p = po4.p(j, "StreamSource(id=", ", type=", str);
        p.append(", link=");
        p.append(str2);
        p.append(", quality=");
        p.append(streamQuality);
        p.append(")");
        return p.toString();
    }
}
