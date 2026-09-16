package wibuku.app.wibuku.model.anime;

import defpackage.ac1;
import defpackage.bp5;
import defpackage.dn0;
import defpackage.mk0;
import defpackage.mn3;
import defpackage.mo1;
import defpackage.nw0;
import defpackage.po4;
import defpackage.qd4;
import defpackage.yg2;
import java.util.Date;
import java.util.List;
import wibuku.app.wibuku.model.user.History;

/* loaded from: classes.dex */
public final class EpisodeDetail {

    @qd4("comments")
    private List<AnimeComment> comments;

    @qd4("created_at")
    private final Date date;

    @qd4("dislikes")
    private final long dislikes;

    @qd4("history")
    private History history;

    @qd4("id")
    private final long id;

    @qd4("likes")
    private final long likes;

    @qd4("name")
    private final String name;

    @qd4("parent_id")
    private final long parent_id;

    @qd4("react")
    private int react;

    @qd4("stream_sources")
    private List<StreamSource> stream_sources;

    @qd4("views")
    private final long views;

    public EpisodeDetail(long j, long j2, String str, long j3, Date date, List<AnimeComment> list, List<StreamSource> list2, long j4, long j5, int i, History history) {
        str.getClass();
        date.getClass();
        list.getClass();
        list2.getClass();
        history.getClass();
        this.id = j;
        this.parent_id = j2;
        this.name = str;
        this.views = j3;
        this.date = date;
        this.comments = list;
        this.stream_sources = list2;
        this.likes = j4;
        this.dislikes = j5;
        this.react = i;
        this.history = history;
    }

    public final long component1() {
        return this.id;
    }

    public final int component10() {
        return this.react;
    }

    public final History component11() {
        return this.history;
    }

    public final long component2() {
        return this.parent_id;
    }

    public final String component3() {
        return this.name;
    }

    public final long component4() {
        return this.views;
    }

    public final Date component5() {
        return this.date;
    }

    public final List<AnimeComment> component6() {
        return this.comments;
    }

    public final List<StreamSource> component7() {
        return this.stream_sources;
    }

    public final long component8() {
        return this.likes;
    }

    public final long component9() {
        return this.dislikes;
    }

    public final EpisodeDetail copy(long j, long j2, String str, long j3, Date date, List<AnimeComment> list, List<StreamSource> list2, long j4, long j5, int i, History history) {
        str.getClass();
        date.getClass();
        list.getClass();
        list2.getClass();
        history.getClass();
        return new EpisodeDetail(j, j2, str, j3, date, list, list2, j4, j5, i, history);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EpisodeDetail)) {
            return false;
        }
        EpisodeDetail episodeDetail = (EpisodeDetail) obj;
        return this.id == episodeDetail.id && this.parent_id == episodeDetail.parent_id && yg2.a(this.name, episodeDetail.name) && this.views == episodeDetail.views && yg2.a(this.date, episodeDetail.date) && yg2.a(this.comments, episodeDetail.comments) && yg2.a(this.stream_sources, episodeDetail.stream_sources) && this.likes == episodeDetail.likes && this.dislikes == episodeDetail.dislikes && this.react == episodeDetail.react && yg2.a(this.history, episodeDetail.history);
    }

    public final List<AnimeComment> getComments() {
        return this.comments;
    }

    public final Date getDate() {
        return this.date;
    }

    public final long getDislikes() {
        return this.dislikes;
    }

    public final History getHistory() {
        return this.history;
    }

    public final long getId() {
        return this.id;
    }

    public final long getLikes() {
        return this.likes;
    }

    public final String getName() {
        return this.name;
    }

    public final long getParent_id() {
        return this.parent_id;
    }

    public final int getReact() {
        return this.react;
    }

    public final List<StreamSource> getStream_sources() {
        return this.stream_sources;
    }

    public final long getViews() {
        return this.views;
    }

    public int hashCode() {
        return this.history.hashCode() + mo1.b(this.react, po4.l(po4.l(po4.m(po4.m(dn0.d(po4.l(mk0.f(this.name, po4.l(Long.hashCode(this.id) * 31, 31, this.parent_id), 31), 31, this.views), this.date, 31), 31, this.comments), 31, this.stream_sources), 31, this.likes), 31, this.dislikes), 31);
    }

    public final void setComments(List<AnimeComment> list) {
        list.getClass();
        this.comments = list;
    }

    public final void setHistory(History history) {
        history.getClass();
        this.history = history;
    }

    public final void setReact(int i) {
        this.react = i;
    }

    public final void setStream_sources(List<StreamSource> list) {
        list.getClass();
        this.stream_sources = list;
    }

    public String toString() {
        long j = this.id;
        long j2 = this.parent_id;
        String str = this.name;
        long j3 = this.views;
        Date date = this.date;
        List<AnimeComment> list = this.comments;
        List<StreamSource> list2 = this.stream_sources;
        long j4 = this.likes;
        long j5 = this.dislikes;
        int i = this.react;
        History history = this.history;
        StringBuilder m = dn0.m(j, "EpisodeDetail(id=", ", parent_id=");
        po4.t(j2, ", name=", str, m);
        dn0.u(j3, ", views=", ", date=", m);
        m.append(date);
        m.append(", comments=");
        m.append(list);
        m.append(", stream_sources=");
        m.append(list2);
        m.append(", likes=");
        m.append(j4);
        dn0.u(j5, ", dislikes=", ", react=", m);
        m.append(i);
        m.append(", history=");
        m.append(history);
        m.append(")");
        return m.toString();
    }

    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    /* JADX WARN: Unknown enum class pattern. Please report as an issue! */
    /* loaded from: classes.dex */
    public static final class Reaction {
        private static final /* synthetic */ ac1 $ENTRIES;
        private static final /* synthetic */ Reaction[] $VALUES;
        public static final Companion Companion;
        private final int type;
        public static final Reaction NONE = new Reaction("NONE", 0, -1);
        public static final Reaction LIKE = new Reaction("LIKE", 1, 0);
        public static final Reaction DISLIKE = new Reaction("DISLIKE", 2, 1);

        private static final /* synthetic */ Reaction[] $values() {
            return new Reaction[]{NONE, LIKE, DISLIKE};
        }

        static {
            Reaction[] $values = $values();
            $VALUES = $values;
            $ENTRIES = bp5.e($values);
            Companion = new Companion(null);
        }

        private Reaction(String str, int i, int i2) {
            this.type = i2;
        }

        public static ac1 getEntries() {
            return $ENTRIES;
        }

        public static Reaction valueOf(String str) {
            return (Reaction) Enum.valueOf(Reaction.class, str);
        }

        public static Reaction[] values() {
            return (Reaction[]) $VALUES.clone();
        }

        public final int getType() {
            return this.type;
        }

        @Override // java.lang.Enum
        public String toString() {
            return String.valueOf(this.type);
        }

        /* loaded from: classes.dex */
        public static final class Companion {
            public /* synthetic */ Companion(nw0 nw0Var) {
                this();
            }

            public final Reaction toReaction(int i) {
                for (Reaction reaction : Reaction.values()) {
                    if (reaction.getType() == i) {
                        return reaction;
                    }
                }
                mn3.y("Array contains no element matching the predicate.");
                return null;
            }

            private Companion() {
            }
        }
    }
}
