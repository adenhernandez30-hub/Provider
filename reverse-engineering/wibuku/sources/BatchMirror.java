package wibuku.app.wibuku.model.anime;

import defpackage.qd4;
import defpackage.yg2;
import java.util.List;

/* loaded from: classes.dex */
public final class BatchMirror {

    @qd4("link")
    private final List<String> link;

    @qd4("quality")
    private final StreamQuality quality;

    public BatchMirror(StreamQuality streamQuality, List<String> list) {
        streamQuality.getClass();
        list.getClass();
        this.quality = streamQuality;
        this.link = list;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ BatchMirror copy$default(BatchMirror batchMirror, StreamQuality streamQuality, List list, int i, Object obj) {
        if ((i & 1) != 0) {
            streamQuality = batchMirror.quality;
        }
        if ((i & 2) != 0) {
            list = batchMirror.link;
        }
        return batchMirror.copy(streamQuality, list);
    }

    public final StreamQuality component1() {
        return this.quality;
    }

    public final List<String> component2() {
        return this.link;
    }

    public final BatchMirror copy(StreamQuality streamQuality, List<String> list) {
        streamQuality.getClass();
        list.getClass();
        return new BatchMirror(streamQuality, list);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BatchMirror)) {
            return false;
        }
        BatchMirror batchMirror = (BatchMirror) obj;
        return this.quality == batchMirror.quality && yg2.a(this.link, batchMirror.link);
    }

    public final List<String> getLink() {
        return this.link;
    }

    public final StreamQuality getQuality() {
        return this.quality;
    }

    public int hashCode() {
        return this.link.hashCode() + (this.quality.hashCode() * 31);
    }

    public String toString() {
        return "BatchMirror(quality=" + this.quality + ", link=" + this.link + ")";
    }
}
