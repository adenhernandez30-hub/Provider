package com.kakaanime.provider

import com.kakaanime.provider.extractor.BrowserStreamResolver

object ProviderFactory {
    fun createRegistry(browserResolver: BrowserStreamResolver? = null): ProviderRegistry {
        return ProviderRegistry().apply {
            register(OtakudesuProvider(browserResolver))
            register(SamehadakuProvider(browserResolver))
            register(AnimeSailProvider(browserResolver))

            register(NativeHtmlProvider("anoboy", "Anoboy", 60, "https://anoboy.xyz", browserResolver))
            register(NativeHtmlProvider("kuronime", "Kuronime", 80, "https://kuronime.net", browserResolver))
            register(NativeHtmlProvider("oploverz", "Oploverz", 200, "https://oploverz.cc", browserResolver))
            register(NativeHtmlProvider("zoronime", "Zoronime", 50, "https://zoronime.com", browserResolver))
            register(AnimeDaoProvider(browserResolver))

            register(NativeHtmlProvider("animasu", "Animasu", 30, "https://www.animasu.my.id", browserResolver))
            register(NativeHtmlProvider("animekompi", "AnimeKompi", 70, "https://animekompi.link", browserResolver))
            register(NativeHtmlProvider("doronime", "Doronime", 100, "https://doroni.me", browserResolver))
            register(NativeHtmlProvider("ylnime", "YLNime", 140, "https://ylnime.com", browserResolver))
            register(NativeHtmlProvider("nontonanimeid", "NontonAnimeID", 150, "https://s13.nontonanimeid.boats", browserResolver))
            register(NativeHtmlProvider("animeisme", "Animeisme", 160, "https://animeisme.net", browserResolver))
            register(NativeHtmlProvider("animeku", "Animeku", 170, "https://animeku.tv", browserResolver))
            register(KuramanimeProvider(browserResolver))
            register(NativeHtmlProvider("wibudesu", "Wibudesu", 220, "https://wibudesu.co", browserResolver))
            register(NativeHtmlProvider("meownime", "Meownime", 230, "https://meownime.ltd", browserResolver))
            register(NativeHtmlProvider("anibatch", "Anibatch", 240, "https://anibatch.anibatch.moe", browserResolver))
            register(NativeHtmlProvider("drivenime", "Drivenime", 260, "https://drivenime.com", browserResolver))
            register(NativeHtmlProvider("anitoki", "Anitoki", 270, "https://anitoki.net", browserResolver))
            register(NativeHtmlProvider("riie", "RiiE", 280, "https://riie.jp", browserResolver))
            register(NativeHtmlProvider("animekuindo", "Animekuindo", 300, "https://animekuindo.live", browserResolver))

            register(RemoteSourceProviderV2("animeindo", "AnimeIndo", 40, "animeindo"))
            register(RemoteSourceProviderV2("hunter-no-sekai", "Hunter no Sekai", 110, "hunter-no-sekai"))
            register(RemoteSourceProviderV2("gomunime", "Gomunime", 120, "gomunime"))
            register(RemoteSourceProviderV2("neonime", "NeoNime", 130, "neonime"))
            register(RemoteSourceProviderV2("nimegami", "Nimegami", 250, "nimegami"))
            register(RemoteSourceProviderV2("kusonime", "Kusonime", 290, "kusonime"))
            register(RemoteSourceProviderV2("allanime", "AllAnime", 320, "allanime"))
        }
    }

    fun createEngine(browserResolver: BrowserStreamResolver? = null): ProviderEngine =
        ProviderEngine(createRegistry(browserResolver))
}
