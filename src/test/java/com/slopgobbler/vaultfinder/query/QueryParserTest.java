package com.slopgobbler.vaultfinder.query;

import com.slopgobbler.vaultfinder.model.ItemEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QueryParser & ItemQuery Test Suite")
public class QueryParserTest {

    @Nested
    @DisplayName("Simple Keyword Searches")
    class SimpleKeywordTests {

        @Test
        @DisplayName("Search single keyword matching item ID")
        void testSingleKeywordMatchingItemId() {
            ItemQuery q = QueryParser.parse("iron");
            ItemEntry ironIngot = new ItemEntry("minecraft:iron_ingot", 64, 0);
            ItemEntry ironSword = new ItemEntry("minecraft:iron_sword", 1, 1);
            ItemEntry diamond = new ItemEntry("minecraft:diamond", 10, 2);

            assertTrue(q.matches(ironIngot), "Expected iron_ingot to match 'iron'");
            assertTrue(q.matches(ironSword), "Expected iron_sword to match 'iron'");
            assertFalse(q.matches(diamond), "Expected diamond not to match 'iron'");
        }

        @Test
        @DisplayName("Search multi-word phrase matching item ID or name")
        void testMultiWordKeywordPhrase() {
            ItemQuery q = QueryParser.parse("diamond sword");
            ItemEntry diaSword = new ItemEntry("minecraft:diamond_sword", 1, 0);
            ItemEntry diaPick = new ItemEntry("minecraft:diamond_pickaxe", 1, 1);
            ItemEntry ironSword = new ItemEntry("minecraft:iron_sword", 1, 2);

            assertTrue(q.matches(diaSword), "Expected diamond_sword to match 'diamond sword'");
            assertFalse(q.matches(diaPick), "Expected diamond_pickaxe not to match 'diamond sword'");
            assertFalse(q.matches(ironSword), "Expected iron_sword not to match 'diamond sword'");
        }

        @Test
        @DisplayName("Search matching custom display name")
        void testCustomNameMatching() {
            ItemQuery q = QueryParser.parse("Slayer");
            ItemEntry swordWithCustomName = new ItemEntry(
                    "minecraft:netherite_sword", 1, "Dragon Slayer",
                    Collections.emptyMap(), 100, 100, 0, Collections.emptySet()
            );
            ItemEntry swordWithoutName = new ItemEntry("minecraft:netherite_sword", 1, 1);

            assertTrue(q.matches(swordWithCustomName), "Expected 'Dragon Slayer' to match 'Slayer'");
            assertFalse(q.matches(swordWithoutName), "Expected unnamed sword not to match 'Slayer'");
        }

        @Test
        @DisplayName("Search is case insensitive")
        void testCaseInsensitivity() {
            ItemQuery upperQ = QueryParser.parse("DIAMOND");
            ItemQuery mixedQ = QueryParser.parse("dIaMoNd SwOrD");
            ItemEntry diaSword = new ItemEntry("minecraft:diamond_sword", 1, 0);

            assertTrue(upperQ.matches(diaSword));
            assertTrue(mixedQ.matches(diaSword));
        }

        @Test
        @DisplayName("Search matches formatted readable name")
        void testReadableNameMatch() {
            ItemQuery q = QueryParser.parse("raw gold");
            ItemEntry rawGold = new ItemEntry("minecraft:raw_gold", 32, 0);

            assertTrue(q.matches(rawGold), "Formatted readable name 'Raw Gold' should match 'raw gold'");
        }
    }

    @Nested
    @DisplayName("Mod Namespace Filter Tests (@mod)")
    class ModNamespaceTests {

        @Test
        @DisplayName("Filter by @minecraft namespace")
        void testMinecraftNamespaceFilter() {
            ItemQuery q = QueryParser.parse("@minecraft");
            ItemEntry vanillaItem = new ItemEntry("minecraft:iron_ingot", 64, 0);
            ItemEntry moddedItem = new ItemEntry("create:cogwheel", 16, 1);

            assertTrue(q.matches(vanillaItem), "minecraft:iron_ingot should match @minecraft");
            assertFalse(q.matches(moddedItem), "create:cogwheel should not match @minecraft");
        }

        @Test
        @DisplayName("Filter by @create namespace")
        void testCreateNamespaceFilter() {
            ItemQuery q = QueryParser.parse("@create");
            ItemEntry createItem = new ItemEntry("create:crushing_wheel", 2, 0);
            ItemEntry vanillaItem = new ItemEntry("minecraft:obsidian", 64, 1);

            assertTrue(q.matches(createItem), "create:crushing_wheel should match @create");
            assertFalse(q.matches(vanillaItem), "minecraft:obsidian should not match @create");
        }

        @Test
        @DisplayName("Combined mod namespace and keyword")
        void testModNamespaceWithKeyword() {
            ItemQuery q = QueryParser.parse("@create cogwheel");
            ItemEntry matchingCog = new ItemEntry("create:cogwheel", 16, 0);
            ItemEntry otherCreate = new ItemEntry("create:shaft", 32, 1);
            ItemEntry vanillaItem = new ItemEntry("minecraft:cogwheel", 1, 2);

            assertTrue(q.matches(matchingCog));
            assertFalse(q.matches(otherCreate));
            assertFalse(q.matches(vanillaItem));
        }

        @Test
        @DisplayName("Negated mod namespace filter (-@minecraft)")
        void testNegatedModNamespaceFilter() {
            ItemQuery q = QueryParser.parse("-@minecraft");
            ItemEntry createItem = new ItemEntry("create:brass_casing", 64, 0);
            ItemEntry vanillaItem = new ItemEntry("minecraft:dirt", 64, 1);

            assertTrue(q.matches(createItem), "Non-minecraft item should match -@minecraft");
            assertFalse(q.matches(vanillaItem), "Vanilla item should be excluded by -@minecraft");
        }
    }

    @Nested
    @DisplayName("Enchantment & Tag Filter Tests (#tag)")
    class EnchantmentTagTests {

        @Test
        @DisplayName("Filter by #mending enchantment tag")
        void testMendingFilter() {
            ItemQuery q = QueryParser.parse("#mending");
            ItemEntry mendedPickaxe = new ItemEntry(
                    "minecraft:diamond_pickaxe", 1, "Miner's Joy",
                    List.of("minecraft:mending", "minecraft:unbreaking"), 100, 0
            );
            ItemEntry plainPickaxe = new ItemEntry(
                    "minecraft:diamond_pickaxe", 1, null,
                    List.of("minecraft:efficiency"), 100, 1
            );

            assertTrue(q.matches(mendedPickaxe), "Item with mending should match #mending");
            assertFalse(q.matches(plainPickaxe), "Item without mending should not match #mending");
        }

        @Test
        @DisplayName("Filter by #unbreaking enchantment tag")
        void testUnbreakingFilter() {
            ItemQuery q = QueryParser.parse("#unbreaking");
            ItemEntry unbreakingBook = new ItemEntry(
                    "minecraft:enchanted_book", 1, null,
                    Map.of("minecraft:unbreaking", 3), 100, 100, 0, Collections.emptySet()
            );
            ItemEntry flameBook = new ItemEntry(
                    "minecraft:enchanted_book", 1, null,
                    Map.of("minecraft:flame", 1), 100, 100, 1, Collections.emptySet()
            );

            assertTrue(q.matches(unbreakingBook));
            assertFalse(q.matches(flameBook));
        }

        @Test
        @DisplayName("Combined enchantment tag and keyword")
        void testTagWithKeyword() {
            ItemQuery q = QueryParser.parse("diamond pickaxe #mending");
            ItemEntry diaPickMending = new ItemEntry(
                    "minecraft:diamond_pickaxe", 1, null,
                    List.of("minecraft:mending"), 100, 0
            );
            ItemEntry diaPickNoMending = new ItemEntry(
                    "minecraft:diamond_pickaxe", 1, null,
                    List.of("minecraft:fortune"), 100, 1
            );
            ItemEntry ironPickMending = new ItemEntry(
                    "minecraft:iron_pickaxe", 1, null,
                    List.of("minecraft:mending"), 100, 2
            );

            assertTrue(q.matches(diaPickMending));
            assertFalse(q.matches(diaPickNoMending));
            assertFalse(q.matches(ironPickMending));
        }

        @Test
        @DisplayName("Custom item category tag filter")
        void testCustomItemCategoryTag() {
            ItemQuery q = QueryParser.parse("#tools");
            ItemEntry toolItem = new ItemEntry(
                    "minecraft:diamond_axe", 1, null,
                    Collections.emptyMap(), 100, 100, 0, Set.of("minecraft:tools", "forge:tools")
            );
            ItemEntry foodItem = new ItemEntry(
                    "minecraft:cooked_beef", 64, null,
                    Collections.emptyMap(), 0, 0, 1, Set.of("minecraft:food")
            );

            assertTrue(q.matches(toolItem));
            assertFalse(q.matches(foodItem));
        }

        @Test
        @DisplayName("Negated tag filter (-#mending)")
        void testNegatedTagFilter() {
            ItemQuery q = QueryParser.parse("-#mending");
            ItemEntry plainItem = new ItemEntry("minecraft:iron_ingot", 64, 0);
            ItemEntry mendedItem = new ItemEntry(
                    "minecraft:bow", 1, null,
                    List.of("minecraft:mending"), 100, 1
            );

            assertTrue(q.matches(plainItem));
            assertFalse(q.matches(mendedItem));
        }
    }

    @Nested
    @DisplayName("Count Comparison Tests (>64, >=128, <10)")
    class CountComparisonTests {

        @Test
        @DisplayName("Strictly greater than comparison (>64)")
        void testGreaterThanComparison() {
            ItemQuery q = QueryParser.parse(">64");
            ItemEntry overStack = new ItemEntry("minecraft:dirt", 65, 0);
            ItemEntry largeStack = new ItemEntry("minecraft:dirt", 128, 1);
            ItemEntry exactStack = new ItemEntry("minecraft:dirt", 64, 2);
            ItemEntry underStack = new ItemEntry("minecraft:dirt", 16, 3);

            assertTrue(q.matches(overStack));
            assertTrue(q.matches(largeStack));
            assertFalse(q.matches(exactStack), "Count 64 should not match strict >64");
            assertFalse(q.matches(underStack));
        }

        @Test
        @DisplayName("Greater than or equal comparison (>=128)")
        void testGreaterThanOrEqualComparison() {
            ItemQuery q = QueryParser.parse(">=128");
            ItemEntry count128 = new ItemEntry("minecraft:cobblestone", 128, 0);
            ItemEntry count256 = new ItemEntry("minecraft:cobblestone", 256, 1);
            ItemEntry count127 = new ItemEntry("minecraft:cobblestone", 127, 2);
            ItemEntry count64 = new ItemEntry("minecraft:cobblestone", 64, 3);

            assertTrue(q.matches(count128), "Count 128 should match >=128");
            assertTrue(q.matches(count256), "Count 256 should match >=128");
            assertFalse(q.matches(count127), "Count 127 should not match >=128");
            assertFalse(q.matches(count64), "Count 64 should not match >=128");
        }

        @Test
        @DisplayName("Strictly less than comparison (<10)")
        void testLessThanComparison() {
            ItemQuery q = QueryParser.parse("<10");
            ItemEntry single = new ItemEntry("minecraft:ender_pearl", 1, 0);
            ItemEntry nine = new ItemEntry("minecraft:ender_pearl", 9, 1);
            ItemEntry ten = new ItemEntry("minecraft:ender_pearl", 10, 2);
            ItemEntry sixteen = new ItemEntry("minecraft:ender_pearl", 16, 3);

            assertTrue(q.matches(single));
            assertTrue(q.matches(nine));
            assertFalse(q.matches(ten), "Count 10 should not match strict <10");
            assertFalse(q.matches(sixteen));
        }

        @Test
        @DisplayName("Less than or equal comparison (<=16)")
        void testLessThanOrEqualComparison() {
            ItemQuery q = QueryParser.parse("<=16");
            ItemEntry count16 = new ItemEntry("minecraft:snowball", 16, 0);
            ItemEntry count8 = new ItemEntry("minecraft:snowball", 8, 1);
            ItemEntry count17 = new ItemEntry("minecraft:snowball", 17, 2);

            assertTrue(q.matches(count16), "Count 16 should match <=16");
            assertTrue(q.matches(count8), "Count 8 should match <=16");
            assertFalse(q.matches(count17), "Count 17 should not match <=16");
        }

        @Test
        @DisplayName("Count comparison with whitespace between operator and value (> 64, >= 128, < 10)")
        void testSpacedCountComparisons() {
            ItemQuery qGt = QueryParser.parse("> 64");
            ItemQuery qGte = QueryParser.parse(">= 128");
            ItemQuery qLt = QueryParser.parse("< 10");

            assertTrue(qGt.matches(new ItemEntry("minecraft:stone", 100, 0)));
            assertFalse(qGt.matches(new ItemEntry("minecraft:stone", 64, 0)));

            assertTrue(qGte.matches(new ItemEntry("minecraft:stone", 128, 0)));
            assertFalse(qGte.matches(new ItemEntry("minecraft:stone", 127, 0)));

            assertTrue(qLt.matches(new ItemEntry("minecraft:stone", 5, 0)));
            assertFalse(qLt.matches(new ItemEntry("minecraft:stone", 10, 0)));
        }

        @Test
        @DisplayName("Prefix syntax 'count>=64' and 'c<10'")
        void testPrefixCountSyntax() {
            ItemQuery q1 = QueryParser.parse("count>=64");
            ItemQuery q2 = QueryParser.parse("c<10");

            assertTrue(q1.matches(new ItemEntry("minecraft:sand", 64, 0)));
            assertFalse(q1.matches(new ItemEntry("minecraft:sand", 32, 0)));

            assertTrue(q2.matches(new ItemEntry("minecraft:sand", 5, 0)));
            assertFalse(q2.matches(new ItemEntry("minecraft:sand", 15, 0)));
        }

        @Test
        @DisplayName("Exact count match (=64)")
        void testExactCountComparison() {
            ItemQuery q = QueryParser.parse("=64");
            ItemEntry exact64 = new ItemEntry("minecraft:coal", 64, 0);
            ItemEntry count63 = new ItemEntry("minecraft:coal", 63, 1);
            ItemEntry count65 = new ItemEntry("minecraft:coal", 65, 2);

            assertTrue(q.matches(exact64));
            assertFalse(q.matches(count63));
            assertFalse(q.matches(count65));
        }
    }

    @Nested
    @DisplayName("Durability & Damaged Filter Tests")
    class DurabilityFilterTests {

        @Test
        @DisplayName("Damaged status filter ('damaged')")
        void testDamagedStatusFilter() {
            ItemQuery q = QueryParser.parse("damaged");
            ItemEntry damagedSword = new ItemEntry(
                    "minecraft:diamond_sword", 1, null,
                    Collections.emptyMap(), 500, 1561, 0, Collections.emptySet()
            );
            ItemEntry freshSword = new ItemEntry(
                    "minecraft:diamond_sword", 1, null,
                    Collections.emptyMap(), 1561, 1561, 1, Collections.emptySet()
            );

            assertTrue(q.matches(damagedSword), "Damaged sword should match 'damaged'");
            assertFalse(q.matches(freshSword), "Full durability sword should not match 'damaged'");
        }

        @Test
        @DisplayName("Undamaged status filter ('undamaged')")
        void testUndamagedStatusFilter() {
            ItemQuery q = QueryParser.parse("undamaged");
            ItemEntry freshPick = new ItemEntry(
                    "minecraft:netherite_pickaxe", 1, null,
                    Collections.emptyMap(), 2031, 2031, 0, Collections.emptySet()
            );
            ItemEntry damagedPick = new ItemEntry(
                    "minecraft:netherite_pickaxe", 1, null,
                    Collections.emptyMap(), 1000, 2031, 1, Collections.emptySet()
            );

            assertTrue(q.matches(freshPick));
            assertFalse(q.matches(damagedPick));
        }

        @Test
        @DisplayName("Durability threshold filter ('durability < 50')")
        void testDurabilityThresholdFilter() {
            ItemQuery q = QueryParser.parse("durability < 50");
            // 45% durability
            ItemEntry lowDur = new ItemEntry(
                    "minecraft:iron_chestplate", 1, null,
                    Collections.emptyList(), 45, 0
            );
            // 80% durability
            ItemEntry highDur = new ItemEntry(
                    "minecraft:iron_chestplate", 1, null,
                    Collections.emptyList(), 80, 1
            );
            // 100% durability
            ItemEntry pristine = new ItemEntry(
                    "minecraft:iron_chestplate", 1, null,
                    Collections.emptyList(), 100, 2
            );

            assertTrue(q.matches(lowDur), "45% durability should match 'durability < 50'");
            assertFalse(q.matches(highDur), "80% durability should not match 'durability < 50'");
            assertFalse(q.matches(pristine), "100% durability should not match 'durability < 50'");
        }

        @Test
        @DisplayName("Durability percent shorthand ('<50%', '>=80%')")
        void testDurabilityPercentShorthand() {
            ItemQuery qLow = QueryParser.parse("<50%");
            ItemQuery qHigh = QueryParser.parse(">=80%");

            ItemEntry item30 = new ItemEntry("minecraft:bow", 1, null, Collections.emptyList(), 30, 0);
            ItemEntry item85 = new ItemEntry("minecraft:bow", 1, null, Collections.emptyList(), 85, 1);

            assertTrue(qLow.matches(item30));
            assertFalse(qLow.matches(item85));

            assertTrue(qHigh.matches(item85));
            assertFalse(qHigh.matches(item30));
        }

        @Test
        @DisplayName("Spaced durability syntax ('durability < 50' vs 'durability<50')")
        void testDurabilitySpacingEquivalence() {
            ItemQuery spaced = QueryParser.parse("durability < 50");
            ItemQuery unspaced = QueryParser.parse("durability<50");
            ItemEntry item40 = new ItemEntry("minecraft:shield", 1, null, Collections.emptyList(), 40, 0);
            ItemEntry item60 = new ItemEntry("minecraft:shield", 1, null, Collections.emptyList(), 60, 1);

            assertEquals(spaced.matches(item40), unspaced.matches(item40));
            assertEquals(spaced.matches(item60), unspaced.matches(item60));
            assertTrue(spaced.matches(item40));
            assertFalse(spaced.matches(item60));
        }

        @Test
        @DisplayName("Combined item name and durability ('diamond sword durability < 50')")
        void testItemWithDurabilityQuery() {
            ItemQuery q = QueryParser.parse("diamond sword durability < 50");
            ItemEntry damagedDiaSword = new ItemEntry(
                    "minecraft:diamond_sword", 1, null,
                    Collections.emptyList(), 35, 0
            );
            ItemEntry freshDiaSword = new ItemEntry(
                    "minecraft:diamond_sword", 1, null,
                    Collections.emptyList(), 100, 1
            );
            ItemEntry damagedIronSword = new ItemEntry(
                    "minecraft:iron_sword", 1, null,
                    Collections.emptyList(), 20, 2
            );

            assertTrue(q.matches(damagedDiaSword));
            assertFalse(q.matches(freshDiaSword));
            assertFalse(q.matches(damagedIronSword));
        }
    }

    @Nested
    @DisplayName("Edge Cases & Robustness Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty string matches all items")
        void testEmptyString() {
            ItemQuery q = QueryParser.parse("");
            ItemEntry apple = new ItemEntry("minecraft:apple", 16, 0);
            ItemEntry sword = new ItemEntry("minecraft:iron_sword", 1, 1);

            assertTrue(q.matches(apple));
            assertTrue(q.matches(sword));
            assertTrue(q.isEmpty());
        }

        @Test
        @DisplayName("Null input string is treated as empty query")
        void testNullInput() {
            ItemQuery q = QueryParser.parse(null);
            ItemEntry item = new ItemEntry("minecraft:torch", 64, 0);

            assertTrue(q.matches(item));
            assertTrue(q.isEmpty());
        }

        @Test
        @DisplayName("Whitespace-only queries are treated as empty")
        void testWhitespaceOnly() {
            ItemQuery q1 = QueryParser.parse("   ");
            ItemQuery q2 = QueryParser.parse("\t\n  \r\n");
            ItemEntry item = new ItemEntry("minecraft:coal", 32, 0);

            assertTrue(q1.matches(item));
            assertTrue(q2.matches(item));
            assertTrue(q1.isEmpty());
            assertTrue(q2.isEmpty());
        }

        @Test
        @DisplayName("Special characters do not throw exceptions")
        void testSpecialCharacters() {
            assertDoesNotThrow(() -> {
                ItemQuery q = QueryParser.parse("!@#$%^&*()_+={}[]|:;<>,.?/~`");
                assertNotNull(q);
            });
        }

        @Test
        @DisplayName("Null ItemEntry passed to matches returns false")
        void testNullItemEntry() {
            ItemQuery q = QueryParser.parse("iron");
            assertFalse(q.matches(null));
        }

        @Test
        @DisplayName("Quoted phrases preserve spaces and require whole phrase")
        void testQuotedPhrases() {
            ItemQuery q = QueryParser.parse("\"Golden Apple\"");
            ItemEntry gApple = new ItemEntry("minecraft:golden_apple", 5, "Golden Apple", Collections.emptyMap(), 0, 0);
            ItemEntry rApple = new ItemEntry("minecraft:apple", 10, "Red Apple", Collections.emptyMap(), 0, 1);

            assertTrue(q.matches(gApple));
            assertFalse(q.matches(rApple));
        }

        @Test
        @DisplayName("Non-matching queries return false")
        void testNonMatchingQuery() {
            ItemQuery q = QueryParser.parse("nonexistent_widget_xyz_999");
            ItemEntry item = new ItemEntry("minecraft:cobblestone", 64, 0);

            assertFalse(q.matches(item));
        }

        @Test
        @DisplayName("Composite multi-criteria query")
        void testCompositeMultiCriteriaQuery() {
            ItemQuery q = QueryParser.parse("@minecraft pickaxe #mending >0 durability<100");

            ItemEntry perfectMatch = new ItemEntry(
                    "minecraft:diamond_pickaxe", 1, null,
                    List.of("minecraft:mending"), 75, 0
            );
            ItemEntry wrongMod = new ItemEntry(
                    "create:crushing_pickaxe", 1, null,
                    List.of("minecraft:mending"), 75, 1
            );
            ItemEntry missingMending = new ItemEntry(
                    "minecraft:diamond_pickaxe", 1, null,
                    List.of("minecraft:unbreaking"), 75, 2
            );
            ItemEntry notDamaged = new ItemEntry(
                    "minecraft:diamond_pickaxe", 1, null,
                    List.of("minecraft:mending"), 100, 3
            );

            assertTrue(q.matches(perfectMatch));
            assertFalse(q.matches(wrongMod));
            assertFalse(q.matches(missingMending));
            assertFalse(q.matches(notDamaged));
        }
    }
}
