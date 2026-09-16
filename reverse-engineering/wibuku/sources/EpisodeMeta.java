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
public final class EpisodeMeta {

    @qd4("comment_count")
    private long comment_count;

    @qd4("comments")
    private List<AnimeComment> comments;

    @qd4("created_at")
    private final Date date;

    @qd4("dislikes")
    private final long dislikes;

    @qd4("giveaways")
    private List<Giveaway> giveaways;

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

    @qd4("user_comments")
    private List<AnimeComment> user_comments;

    @qd4("views")
    private final long views;

    public EpisodeMeta(long j, long j2, String str, long j3, Date date, long j4, List<AnimeComment> list, List<AnimeComment> list2, List<StreamSource> list3, long j5, long j6, int i, History history, List<Giveaway> list4) {
        str.getClass();
        date.getClass();
        list.getClass();
        list2.getClass();
        list3.getClass();
        history.getClass();
        this.id = j;
        this.parent_id = j2;
        this.name = str;
        this.views = j3;
        this.date = date;
        this.comment_count = j4;
        this.comments = list;
        this.user_comments = list2;
        this.stream_sources = list3;
        this.likes = j5;
        this.dislikes = j6;
        this.react = i;
        this.history = history;
        this.giveaways = list4;
    }

    public static /* synthetic */ EpisodeMeta copy$default(EpisodeMeta episodeMeta, long j, long j2, String str, long j3, Date date, long j4, List list, List list2, List list3, long j5, long j6, int i, History history, List list4, int i2, Object obj) {
        long j7;
        long j8;
        long j9 = (i2 & 1) != 0 ? episodeMeta.id : j;
        long j10 = (i2 & 2) != 0 ? episodeMeta.parent_id : j2;
        String str2 = (i2 & 4) != 0 ? episodeMeta.name : str;
        long j11 = (i2 & 8) != 0 ? episodeMeta.views : j3;
        Date date2 = (i2 & 16) != 0 ? episodeMeta.date : date;
        long j12 = (i2 & 32) != 0 ? episodeMeta.comment_count : j4;
        List list5 = (i2 & 64) != 0 ? episodeMeta.comments : list;
        List list6 = (i2 & 128) != 0 ? episodeMeta.user_comments : list2;
        List list7 = (i2 & 256) != 0 ? episodeMeta.stream_sources : list3;
        if ((i2 & 512) != 0) {
            j7 = j9;
            j8 = episodeMeta.likes;
        } else {
            j7 = j9;
            j8 = j5;
        }
        return episodeMeta.copy(j7, j10, str2, j11, date2, j12, list5, list6, list7, j8, (i2 & 1024) != 0 ? episodeMeta.dislikes : j6, (i2 & 2048) != 0 ? episodeMeta.react : i, (i2 & 4096) != 0 ? episodeMeta.history : history, (i2 & 8192) != 0 ? episodeMeta.giveaways : list4);
    }

    public final long component1() {
        return this.id;
    }

    public final long component10() {
        return this.likes;
    }

    public final long component11() {
        return this.dislikes;
    }

    public final int component12() {
        return this.react;
    }

    public final History component13() {
        return this.history;
    }

    public final List<Giveaway> component14() {
        return this.giveaways;
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

    public final long component6() {
        return this.comment_count;
    }

    public final List<AnimeComment> component7() {
        return this.comments;
    }

    public final List<AnimeComment> component8() {
        return this.user_comments;
    }

    public final List<StreamSource> component9() {
        return this.stream_sources;
    }

    public final EpisodeMeta copy(long j, long j2, String str, long j3, Date date, long j4, List<AnimeComment> list, List<AnimeComment> list2, List<StreamSource> list3, long j5, long j6, int i, History history, List<Giveaway> list4) {
        str.getClass();
        date.getClass();
        list.getClass();
        list2.getClass();
        list3.getClass();
        history.getClass();
        return new EpisodeMeta(j, j2, str, j3, date, j4, list, list2, list3, j5, j6, i, history, list4);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EpisodeMeta)) {
            return false;
        }
        EpisodeMeta episodeMeta = (EpisodeMeta) obj;
        return this.id == episodeMeta.id && this.parent_id == episodeMeta.parent_id && yg2.a(this.name, episodeMeta.name) && this.views == episodeMeta.views && yg2.a(this.date, episodeMeta.date) && this.comment_count == episodeMeta.comment_count && yg2.a(this.comments, episodeMeta.comments) && yg2.a(this.user_comments, episodeMeta.user_comments) && yg2.a(this.stream_sources, episodeMeta.stream_sources) && this.likes == episodeMeta.likes && this.dislikes == episodeMeta.dislikes && this.react == episodeMeta.react && yg2.a(this.history, episodeMeta.history) && yg2.a(this.giveaways, episodeMeta.giveaways);
    }

    public final long getComment_count() {
        return this.comment_count;
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

    public final List<Giveaway> getGiveaways() {
        return this.giveaways;
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

    public final List<AnimeComment> getUser_comments() {
        return this.user_comments;
    }

    public final long getViews() {
        return this.views;
    }

    public int hashCode() {
        int hashCode;
        int hashCode2 = (this.history.hashCode() + mo1.b(this.react, po4.l(po4.l(po4.m(po4.m(po4.m(po4.l(dn0.d(po4.l(mk0.f(this.name, po4.l(Long.hashCode(this.id) * 31, 31, this.parent_id), 31), 31, this.views), this.date, 31), 31, this.comment_count), 31, this.comments), 31, this.user_comments), 31, this.stream_sources), 31, this.likes), 31, this.dislikes), 31)) * 31;
        List<Giveaway> list = this.giveaways;
        if (list == null) {
            hashCode = 0;
        } else {
            hashCode = list.hashCode();
        }
        return hashCode2 + hashCode;
    }

    public final void setComment_count(long j) {
        this.comment_count = j;
    }

    public final void setComments(List<AnimeComment> list) {
        list.getClass();
        this.comments = list;
    }

    public final void setGiveaways(List<Giveaway> list) {
        this.giveaways = list;
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

    public final void setUser_comments(List<AnimeComment> list) {
        list.getClass();
        this.user_comments = list;
    }

    public String toString() {
        long j = this.id;
        long j2 = this.parent_id;
        String str = this.name;
        long j3 = this.views;
        Date date = this.date;
        long j4 = this.comment_count;
        List<AnimeComment> list = this.comments;
        List<AnimeComment> list2 = this.user_comments;
        List<StreamSource> list3 = this.stream_sources;
        long j5 = this.likes;
        long j6 = this.dislikes;
        int i = this.react;
        History history = this.history;
        List<Giveaway> list4 = this.giveaways;
        StringBuilder m = dn0.m(j, "EpisodeMeta(id=", ", parent_id=");
        po4.t(j2, ", name=", str, m);
        dn0.u(j3, ", views=", ", date=", m);
        m.append(date);
        m.append(", comment_count=");
        m.append(j4);
        m.append(", comments=");
        m.append(list);
        m.append(", user_comments=");
        m.append(list2);
        m.append(", stream_sources=");
        m.append(list3);
        m.append(", likes=");
        m.append(j5);
        dn0.u(j6, ", dislikes=", ", react=", m);
        m.append(i);
        m.append(", history=");
        m.append(history);
        m.append(", giveaways=");
        m.append(list4);
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

    public /* synthetic */ EpisodeMeta(long j, long j2, String str, long j3, Date date, long j4, List list, List list2, List list3, long j5, long j6, int i, History history, List list4, int i2, nw0 nw0Var) {
        this(j, j2, str, j3, date, j4, list, list2, list3, j5, j6, i, history, (i2 & 8192) != 0 ? null : list4);
    }
}
