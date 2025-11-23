package com.charactor.avatar.maker.pfp.activity_app.posterwanted

/**
 * Data model for Poster Wanted Template item
 */
data class PosterWantedItem(
    val id: Int,
    val templateId: Int,        // 1-16
    val avatarId: Int,          // 1-25
    val name: String,
    val bounty: String
) {
    /**
     * Get avatar asset path for Glide
     */
    fun getAvatarPath(): String {
        return "file:///android_asset/avatar/$avatarId.png"
    }

    /**
     * Get template item path for Glide
     */
    fun getTemplatePath(): String {
        return "file:///android_asset/template/$templateId/item.png"
    }

    companion object {
        // Random pirate names
        private val PIRATE_NAMES = listOf(
            "MONKEY D. LUFFY",
            "RORONOA ZORO",
            "NAMI",
            "USOPP",
            "SANJI",
            "TONY CHOPPER",
            "NICO ROBIN",
            "FRANKY",
            "BROOK",
            "JINBE",
            "SHANKS",
            "BLACKBEARD",
            "KAIDO",
            "BIG MOM",
            "WHITEBEARD",
            "GOL D. ROGER",
            "PORTGAS D. ACE",
            "TRAFALGAR LAW",
            "EUSTASS KID",
            "DRACULE MIHAWK",
            "BOA HANCOCK",
            "CROCODILE",
            "DONQUIXOTE",
            "KATAKURI",
            "MARCO",
            "SABO",
            "BUGGY",
            "SMOKER",
            "AOKIJI",
            "KIZARU",
            "AKAINU",
            "FUJITORA",
            "GARP",
            "SENGOKU",
            "RAYLEIGH",
            "CAPTAIN JACK",
            "BLACKHEART",
            "REDBEARD",
            "STORMWIND",
            "SEAWOLF"
        )

        // Random bounty amounts
        private val BOUNTY_AMOUNTS = listOf(
            "$30,000,000",
            "$50,000,000",
            "$100,000,000",
            "$200,000,000",
            "$300,000,000",
            "$500,000,000",
            "$800,000,000",
            "$1,00,000,000",
            "$1,50,000,00",
            "$2,50,000,000",
            "$3,50,000,000",
            "$4,000,000",
            "$550,000,000",
            "$10,000,000",
            "$15,000,000",
            "$66,000,000",
            "$77,000,000",
            "$88,000,000",
            "$320,000,000",
            "$438,000,000"
        )

        /**
         * Generate random poster wanted items
         */
        fun generateRandomItems(count: Int): List<PosterWantedItem> {
            val items = mutableListOf<PosterWantedItem>()
            val random = java.util.Random()

            for (i in 1..count) {
                items.add(
                    PosterWantedItem(
                        id = i,
                        templateId = random.nextInt(16) + 1,      // 1-16
                        avatarId = random.nextInt(25) + 1,        // 1-25
                        name = PIRATE_NAMES[random.nextInt(PIRATE_NAMES.size)],
                        bounty = BOUNTY_AMOUNTS[random.nextInt(BOUNTY_AMOUNTS.size)]
                    )
                )
            }

            return items
        }
    }
}
