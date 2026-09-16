package wibuku.app.wibuku.model.anime;

import defpackage.qd4;
import defpackage.yg2;

/* loaded from: classes.dex */
public final class LoadedStreamSource {

    @qd4("direct")
    private final String direct;

    @qd4("stream_source")
    private final StreamSource streamSource;

    public LoadedStreamSource(StreamSource streamSource, String str) {
        streamSource.getClass();
        str.getClass();
        this.streamSource = streamSource;
        this.direct = str;
    }

    public static /* synthetic */ LoadedStreamSource copy$default(LoadedStreamSource loadedStreamSource, StreamSource streamSource, String str, int i, Object obj) {
        if ((i & 1) != 0) {
            streamSource = loadedStreamSource.streamSource;
        }
        if ((i & 2) != 0) {
            str = loadedStreamSource.direct;
        }
        return loadedStreamSource.copy(streamSource, str);
    }

    public final StreamSource component1() {
        return this.streamSource;
    }

    public final String component2() {
        return this.direct;
    }

    public final LoadedStreamSource copy(StreamSource streamSource, String str) {
        streamSource.getClass();
        str.getClass();
        return new LoadedStreamSource(streamSource, str);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LoadedStreamSource)) {
            return false;
        }
        LoadedStreamSource loadedStreamSource = (LoadedStreamSource) obj;
        return yg2.a(this.streamSource, loadedStreamSource.streamSource) && yg2.a(this.direct, loadedStreamSource.direct);
    }

    public final String getDirect() {
        return this.direct;
    }

    public final StreamSource getStreamSource() {
        return this.streamSource;
    }

    public int hashCode() {
        return this.direct.hashCode() + (this.streamSource.hashCode() * 31);
    }

    public String toString() {
        return "LoadedStreamSource(streamSource=" + this.streamSource + ", direct=" + this.direct + ")";
    }
}
