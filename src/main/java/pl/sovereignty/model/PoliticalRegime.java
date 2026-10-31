package pl.sovereignty.model;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum PoliticalRegime {
    // 1. Klasyczne i Średniowieczne
    HEREDITARY_MONARCHY(
            "Monarchia Dziedziczna",
            Material.GOLDEN_HELMET,
            1.05,  // +5% ogólna produkcja
            0.90,  // -10% koszt murów
            1.00,  // standardowy upkeep
            900,   // 15 min cooldown claimu
            1200.0, // koszt kryzysu dynastycznego przy zmianie (obniżony z 5000 $)
            Arrays.asList(
                    "&a[BUFF] Odporność (Resistance I) w stolicy oraz +5% do produkcji.",
                    "&a[BUFF] -10% do kosztu budowy murów obronnych.",
                    "&8Zasada Korony: silna centralizacja i dyscyplina rodu.",
                    "&c[DEBUFF] Zmiana ustroju wywołuje Kryzys Dynastyczny (-1 200 $).",
                    "&c[DEBUFF] Spowolnienie poza granicami bez pełnego nasycenia."
            )
    ),
    MERCHANT_REPUBLIC(
            "Republika Handlowa",
            Material.EMERALD,
            1.00,
            1.00,
            0.80,  // -20% upkeep
            900,
            500.0, // obniżony z 1500 $
            Arrays.asList(
                    "&a[BUFF] Szybkość (Speed I) dla wszystkich obywateli.",
                    "&a[BUFF] -20% do dziennego podatku gruntowego (Upkeep) i +15% zysku ze sprzedaży.",
                    "&8Kupcy i gildie obniżają podatki gruntowe.",
                    "&c[DEBUFF] Pacyfizm handlowy: -10% zadawanych obrażeń w walkach PvP.",
                    "&c[DEBUFF] Brak przyspieszenia produkcji surowców podstawowych."
            )
    ),
    MILITARISM(
            "Militaryzm",
            Material.NETHERITE_SWORD,
            1.00,
            1.00,
            1.00,
            600,   // 10 min cooldown claimu
            800.0, // obniżony z 2500 $
            Arrays.asList(
                    "&a[BUFF] Siła (Strength I) podczas obrony terytorium przed intruzami.",
                    "&a[BUFF] Skrócenie asymilacji chunków z 15 do 10 minut i +25% HP murów.",
                    "&8Doktryna ekspansji i mobilizacji wojennej.",
                    "&c[DEBUFF] Apetyt wojenny: głód spada szybciej (Głód I).",
                    "&c[DEBUFF] -5% do produkcji rolnej (rekwirowanie rąk do wojska)."
            )
    ),
    TECHNOCRACY(
            "Technokracja",
            Material.REDSTONE_BLOCK,
            1.00,
            1.00,
            1.25,  // +25% upkeep
            900,
            1000.0, // obniżony z 3000 $
            Arrays.asList(
                    "&a[BUFF] Pośpiech (Haste I) do kopania oraz Widzenie w ciemności (Night Vision).",
                    "&a[BUFF] -20% do cen wszystkich ulepszeń w drzewku technologii.",
                    "&8Rządy inżynierów i mistrzów cechowych.",
                    "&c[DEBUFF] Wysokie koszty aparatury: +25% do dziennego Upkeepu chunków.",
                    "&c[DEBUFF] Słabsza regeneracja pancerza w walce wręcz."
            )
    ),
    THEOCRACY(
            "Teokracja / Zakon Rycerski",
            Material.TOTEM_OF_UNDYING,
            1.02,
            1.00,
            0.90,
            900,
            1200.0, // obniżony z 4000 $
            Arrays.asList(
                    "&a[BUFF] Regeneracja (Regeneration I) na ziemiach świętych (własny teren).",
                    "&a[BUFF] -30% częstotliwości najazdów piratów (błogosławieństwo wyspy).",
                    "&8Wiara i oddanie zakonnej wspólnocie.",
                    "&c[DEBUFF] Spowolnienie (Slowness I) przy spadku zdrowia poniżej 50% (pokuta ascetyczna).",
                    "&c[DEBUFF] Rygor monetarny: zakaz bicia zdeprecjonowanej waluty."
            )
    ),
    AUTOCRACY(
            "Autokracja / Imperium",
            Material.NETHERITE_CHESTPLATE,
            1.03,
            0.95,
            1.10,
            750,   // 12.5 min cooldown claimu
            1500.0, // obniżony z 6000 $
            Arrays.asList(
                    "&a[BUFF] Odporność na ogień (Fire Resistance) oraz Absorpcja serc w bastionie.",
                    "&a[BUFF] +15 kratek do zasięgu wpływów Bastionu i Strażnic.",
                    "&8Żelazna ręka monarchy i potęga imperialna.",
                    "&c[DEBUFF] Zmęczenie górnicze (Mining Fatigue I) pod ziemią poniżej poziomu Y=40.",
                    "&c[DEBUFF] -10% zadowolenia mieszkańców (surowy reżim podatkowy)."
            )
    ),
    AGRARIAN_COMMUNE(
            "Komuna Rolniczo-Robotnicza",
            Material.HAY_BLOCK,
            1.10,  // +10%
            1.00,
            0.85,  // -15% upkeep
            900,
            300.0, // obniżony z 1000 $
            Arrays.asList(
                    "&a[BUFF] Pośpiech (Haste I) na farmach oraz zwiększona produkcja żywności.",
                    "&a[BUFF] +20% do zbiorów z Farm Państwowych i +10% drewna z Tartaków.",
                    "&8Braterstwo pracy i wspólnota plonów ziemi.",
                    "&c[DEBUFF] Pacyfizm robotniczy: -15% zadawanych obrażeń w walkach PvP.",
                    "&c[DEBUFF] -15% do obrotu handlowego i emisji złotej waluty."
            )
    ),

    // 2. Frakcyjne i Wyspiarskie
    CORSAIR_SYNDICATE(
            "Syndykat Korsarski (Przystań Piratów)",
            Material.SPYGLASS,
            1.00,
            2.00,  // 2x koszt murów
            0.75,  // -25% upkeep
            900,
            700.0, // obniżony z 2000 $
            Arrays.asList(
                    "&a[BUFF] Łaska delfina, Oddychanie pod wodą i Szybkość w wodzie/na plażach.",
                    "&a[BUFF] Podwójne łupy ze skrzyń pokonanych desantów morskich i +50% cła ze statków.",
                    "&8Panowanie nad morskimi szlakami i swoboda żeglugi.",
                    "&c[DEBUFF] Zmęczenie górnicze (Mining Fatigue I) pod ziemią (piraci nienawidzą kopalń!).",
                    "&c[DEBUFF] 2x wyższy koszt budowy murów obronnych (stawiają na flotę)."
            )
    ),
    FEUDAL_REALM(
            "Feudalizm / Księstwo Senioralne",
            Material.IRON_CHESTPLATE,
            1.08,  // +8% z lenn
            0.95,
            0.95,
            800,
            800.0, // obniżony z 2500 $
            Arrays.asList(
                    "&a[BUFF] Odporność (Resistance I) na własnym terenie i Szybkość II na koniu.",
                    "&a[BUFF] +8% do ogólnej produkcji surowców z lenn państwowych.",
                    "&8Tradycja rycerska, przysięgi lenne i honor herbowy.",
                    "&c[DEBUFF] Przeciążenie rycerskie: Spowolnienie (Slowness I) pieszo w pełnym pancerzu.",
                    "&c[DEBUFF] Wymóg honoru: brak zysków z handlu pirackiego."
            )
    ),
    TRIBAL_FEDERATION(
            "Federacja Plemienna / Klany Wysp",
            Material.BOW,
            1.05,
            1.30,
            0.60,  // -40% upkeep
            450,   // 7.5 min cooldown
            500.0, // obniżony z 1500 $
            Arrays.asList(
                    "&a[BUFF] Stała Szybkość (Speed I) i Skok (Jump Boost I) dla klanowiczów.",
                    "&a[BUFF] Błyskawiczna kolonizacja (7.5 min cooldown) i -40% do Upkeepu terenu.",
                    "&8Więź z naturą wysp i wspólnota wojennych klanów.",
                    "&c[DEBUFF] Pogarda dla ciężkich metali: Osłabienie (Weakness I) w zbroi diamentowej/netherytowej.",
                    "&c[DEBUFF] +30% do kosztu budowy murów z kamienia."
            )
    ),
    PLUTOCRACY(
            "Plutokracja / Konsorcjum Finansowe",
            Material.GOLD_INGOT,
            1.00,
            1.15,
            0.70,  // -30% upkeep
            900,
            1200.0, // obniżony z 3500 $
            Arrays.asList(
                    "&a[BUFF] Szczęście (Luck II) oraz Bohater Wioski (tanie ceny u handlarzy).",
                    "&a[BUFF] -30% do Upkeepu chunków i szansa na dodatkowe samorodki złota przy wydobyciu.",
                    "&8Władza kapitału, giełd i potęgi bankierskiej.",
                    "&c[DEBUFF] Pospolite ruszenie bez doświadczenia: Osłabienie (Weakness I) w walce wręcz.",
                    "&c[DEBUFF] +15% do kosztów fortyfikacji obronnych."
            )
    ),
    MYSTIC_MAGOCRACY(
            "Magokracja / Akademia Magii",
            Material.ENCHANTED_BOOK,
            1.04,
            1.20,
            1.10,
            750,   // 12.5 min
            1000.0, // obniżony z 3000 $
            Arrays.asList(
                    "&a[BUFF] Widzenie w nocy (Night Vision) i Powolne opadanie (Slow Falling) przy upadku.",
                    "&a[BUFF] 2x więcej punktów doświadczenia (EXP) ze wszystkich zabitych potworów.",
                    "&8Rada arcymagów badająca prastare sekrety wysp.",
                    "&c[DEBUFF] Szklana armata: otrzymuje +20% więcej obrażeń fizycznych od broni białej.",
                    "&c[DEBUFF] +20% koszt budowy konwencjonalnych murów."
            )
    ),
    ANARCHO_SYNDICALISM(
            "Anarcho-Syndykat Rzemieślników",
            Material.CAMPFIRE,
            1.15,  // +15% produkcja
            1.40,  // mury droższe
            0.80,  // -20% upkeep
            700,
            300.0, // obniżony z 1000 $
            Arrays.asList(
                    "&a[BUFF] Pośpiech II (Haste II) na całym terytorium nacji dla wszystkich pracowników.",
                    "&a[BUFF] +15% do wydobycia z kopalń i tartaków, niska opłata zmiany ustroju (300 $).",
                    "&8Wolna praca rzemieślników bez ucisku arystokracji.",
                    "&c[DEBUFF] Wrażliwość militarna: -10% punktów pancerza i +40% kosztu budowy murów.",
                    "&c[DEBUFF] Zakaz konstruowania machin oblężniczych najwyższego tieru."
            )
    ),
    SPARTAN_STRATOCRACY(
            "Spartańska Stratokracja",
            Material.SHIELD,
            0.85,  // -15% cywilna produkcja
            0.80,  // -20% mury
            1.10,
            500,   // ~8.3 min
            1000.0, // obniżony z 3000 $
            Arrays.asList(
                    "&a[BUFF] Stała Siła (Strength I) w granicach państwa i +15% zadawanych obrażeń.",
                    "&a[BUFF] Regeneracja II na 5 sekund po każdym zabójstwie w walce, -20% koszt murów.",
                    "&8Kultura wojowników, żelazna spartańska tarcza i kult męstwa.",
                    "&c[DEBUFF] Wilczy apetyt: głód spada o 25% szybciej (Głód I w trakcie marszu).",
                    "&c[DEBUFF] -15% do plonów ze struktur rolnych (gardzenie pracą na roli)."
            )
    ),

    // 3. Starożytne i Współczesne Ustrojowe
    CONSTITUTIONAL_DEMOCRACY(
            "Demokracja Parlamentarna",
            Material.BOOKSHELF,
            1.05,  // +5%
            1.00,
            0.90,  // -10% upkeep
            1050,  // 17.5 min (biurokracja)
            700.0, // obniżony z 2000 $
            Arrays.asList(
                    "&a[BUFF] Regeneracja (Regeneration I) w stolicach i miastach nacji (wysokie morale).",
                    "&a[BUFF] Bohater Wioski (Hero of the Village) i -10% do podatku gruntowego Upkeep.",
                    "&8Wola obywateli, parlamentaryzm i wolność słowa.",
                    "&c[DEBUFF] Biurokracja demokratyczna: asymilacja chunków wydłużona do 17.5 minut.",
                    "&c[DEBUFF] Zakaz autokratycznych dekretów wojennych bez debaty."
            )
    ),
    CONSTITUTIONAL_REPUBLIC(
            "Republika Obywatelska",
            Material.LANTERN,
            1.04,
            0.90,  // -10% mury
            0.75,  // -25% upkeep
            900,
            800.0, // obniżony z 2500 $
            Arrays.asList(
                    "&a[BUFF] Odporność (Resistance I) dla wszystkich obywateli broniących granic prawa.",
                    "&a[BUFF] -25% do opłat gruntowych Upkeep oraz -10% kosztu budowy murów obronnych.",
                    "&8Rządy prawa, instytucje obywatelskie i podział władzy.",
                    "&c[DEBUFF] Rygor legislacyjny: zakaz handlu nielegalnymi łupami korsarskimi.",
                    "&c[DEBUFF] Wyższy koszt proceduralny wypowiedzenia wojny."
            )
    ),
    TOTALITARIAN_STATE(
            "Reżim Totalitarny (Totalitaryzm)",
            Material.IRON_BARS,
            1.10,  // +10% produkcja wojenna
            0.80,  // -20% mury
            1.15,  // +15% upkeep
            600,   // 10 min
            1400.0, // obniżony z 4000 $
            Arrays.asList(
                    "&a[BUFF] Inwigilacja graniczna: wrodzy intruzi otrzymują Świecenie (Glowing) na 10s.",
                    "&a[BUFF] -20% koszt budowy murów obronnych, asymilacja skrócona do 10 minut.",
                    "&8Żelazna dyscyplina, kult państwa i powszechna inwigilacja.",
                    "&c[DEBUFF] Izolacja handlowa: -20% zysków ze sprzedaży surowców obcym kupcom.",
                    "&c[DEBUFF] Zadłużenie państwa wywołuje natychmiastowy bunt ludowy."
            )
    ),
    POLICE_STATE(
            "Państwo Policyjne",
            Material.CHAIN,
            1.00,
            0.85,
            1.30,  // +30% upkeep
            800,
            1200.0, // obniżony z 3500 $
            Arrays.asList(
                    "&a[BUFF] Odporność I oraz Siła I dla obrońców w promieniu 40 bloków od Bastionu.",
                    "&a[BUFF] Detektory policyjne: automatyczne demaskowanie niewidzialnych szpiegów!",
                    "&8Aparat bezpieczeństwa, eliminacja sabotażystów i kontrola porządku.",
                    "&c[DEBUFF] Ogromny koszt aparatu kontroli: +30% do dziennego podatku Upkeep.",
                    "&c[DEBUFF] Spadek zadowolenia obywateli i powolniejszy napływ nowych osadników."
            )
    ),
    EXPANSIONIST_HEGEMONY(
            "Ekspansjonistyczna Hegemonia",
            Material.CROSSBOW,
            0.95,
            0.85,
            1.00,
            450,   // 7.5 min claim
            1200.0, // obniżony z 3500 $
            Arrays.asList(
                    "&a[BUFF] Agresywna asymilacja ziem co 7.5 minuty i Siła I podczas natarcia oblężniczego.",
                    "&a[BUFF] Machiny wojenne hegemona zadają +25% obrażeń murom wrogich nacji.",
                    "&8Doktryna imperialnego podboju i bezwzględnej dominacji na morzach.",
                    "&c[DEBUFF] Wrogość archipelagu: +40% częstotliwości desantów piratów i buntowników.",
                    "&c[DEBUFF] Niemożność zawierania paktów sojuszniczych z wolnymi wyspami."
            )
    ),
    MERITOCRACY(
            "Merytokracja (Rządy Ekspertów)",
            Material.COMPASS,
            1.12,  // +12% produkcja
            0.90,  // -10% mury
            0.85,  // -15% upkeep
            750,   // 12.5 min
            1000.0, // obniżony z 3000 $
            Arrays.asList(
                    "&a[BUFF] Pośpiech (Haste I) na terenie nacji i -15% do cen w drzewku technologii.",
                    "&a[BUFF] +12% do wydajności tartaków, kopalń i farm dzięki naukowej optymalizacji.",
                    "&8Rządy najzdolniejszych inżynierów, naukowców i mistrzów rzemiosła.",
                    "&c[DEBUFF] Ograniczenia elitarne: podwyższony koszt rejestracji nowych obywateli.",
                    "&c[DEBUFF] Brak mistycznych i religijnych błogosławieństw ochronnych."
            )
    ),
    CLASSICAL_OLIGARCHY(
            "Oligarchia / Rada Patrycjuszy",
            Material.GOLDEN_APPLE,
            1.06,  // +6%
            1.10,
            0.80,  // -20% upkeep
            850,
            1200.0, // obniżony z 4000 $
            Arrays.asList(
                    "&a[BUFF] Absorpcja (dodatkowe serca) po powrocie do stolicy i +10% zysków z banku.",
                    "&a[BUFF] -20% do opłat za utrzymanie chunków (kapitał patrycjuszy).",
                    "&8Władza rodów arystokratycznych, rad senatorskich i patrycjatu.",
                    "&c[DEBUFF] Niezadowolenie plebejuszy: Spowolnienie (Slowness I) poza stolicą.",
                    "&c[DEBUFF] +10% do kosztu budowy konwencjonalnych murów obronnych."
            )
    );

    private final String displayName;
    private final Material icon;
    private final double productionMultiplier;
    private final double wallCostMultiplier;
    private final double upkeepMultiplier;
    private final int claimCooldownSeconds;
    private final double transitionCost;
    private final List<String> lore;

    PoliticalRegime(String displayName, Material icon, double productionMultiplier,
                    double wallCostMultiplier, double upkeepMultiplier, int claimCooldownSeconds,
                    double transitionCost, List<String> lore) {
        this.displayName = displayName;
        this.icon = icon;
        this.productionMultiplier = productionMultiplier;
        this.wallCostMultiplier = wallCostMultiplier;
        this.upkeepMultiplier = upkeepMultiplier;
        this.claimCooldownSeconds = claimCooldownSeconds;
        this.transitionCost = transitionCost;
        this.lore = lore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public double getProductionMultiplier() {
        return productionMultiplier;
    }

    public double getWallCostMultiplier() {
        return wallCostMultiplier;
    }

    public double getUpkeepMultiplier() {
        return upkeepMultiplier;
    }

    public int getClaimCooldownSeconds() {
        return claimCooldownSeconds;
    }

    public double getTransitionCost() {
        return transitionCost;
    }

    public List<String> getLore() {
        return lore;
    }
}
