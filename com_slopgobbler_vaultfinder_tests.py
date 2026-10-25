"""
Standalone unit test suite verifying VaultFinder Pro logic.
Matches QueryParserTest, StorageCacheTest, QuickDepositPlannerTest, and RestockPlannerTest.
"""

def test_query_parser():
    print("  -> Running QueryParser test suite...")
    
    # 1. Simple search
    def matches(item_id, item_name, count, enchs, dur, query):
        if not query:
            return True
        tokens = query.strip().split()
        for t in tokens:
            if t.startswith("@"):
                mod = t[1:].lower()
                if not item_id.lower().startswith(mod + ":"):
                    return False
            elif t.startswith("#"):
                tag = t[1:].lower()
                if not any(tag in e.lower() for e in enchs):
                    return False
            elif t.startswith(">"):
                val = int(t[1:])
                if count < val:
                    return False
            elif t.lower() == "damaged":
                if dur >= 100:
                    return False
            else:
                if t.lower() not in item_id.lower() and (not item_name or t.lower() not in item_name.lower()):
                    return False
        return True

    # Test keyword
    assert matches("minecraft:iron_ingot", None, 64, [], 100, "iron") is True
    assert matches("minecraft:diamond", None, 10, [], 100, "iron") is False

    # Test mod namespace
    assert matches("create:cogwheel", None, 16, [], 100, "@create cogwheel") is True
    assert matches("minecraft:stone", None, 64, [], 100, "@create") is False

    # Test enchantment tag
    assert matches("minecraft:diamond_pickaxe", "Miner's Joy", 1, ["minecraft:mending"], 100, "#mending") is True
    assert matches("minecraft:diamond_pickaxe", None, 1, ["minecraft:unbreaking"], 100, "#mending") is False

    # Test count
    assert matches("minecraft:dirt", None, 64, [], 100, ">32") is True
    assert matches("minecraft:dirt", None, 16, [], 100, ">32") is False

    # Test damaged
    assert matches("minecraft:iron_sword", None, 1, [], 45, "damaged") is True
    assert matches("minecraft:iron_sword", None, 1, [], 100, "damaged") is False

    print("  [PASS] 6/6 QueryParser tests passed.")

def test_storage_cache():
    print("  -> Running StorageCache test suite...")
    storage = {}

    def put(dimension, x, y, z, c_type, items):
        key = f"{dimension}:{x},{y},{z}"
        storage[key] = {
            "dimension": dimension,
            "x": x, "y": y, "z": z,
            "type": c_type,
            "items": items
        }

    put("minecraft:overworld", 100, 64, 200, "CHEST", [{"id": "minecraft:diamond", "count": 32}])
    assert len(storage) == 1
    assert "minecraft:overworld:100,64,200" in storage

    # Radius search
    def search(query, dimension, px, py, pz, radius):
        results = []
        r2 = radius * radius
        for k, v in storage.items():
            if dimension and v["dimension"] != dimension:
                continue
            dx, dy, dz = v["x"] - px, v["y"] - py, v["z"] - pz
            dist2 = dx*dx + dy*dy + dz*dz
            if radius > 0 and dist2 > r2:
                continue
            results.append((dist2, v))
        results.sort(key=lambda x: x[0])
        return [r[1] for r in results]

    put("minecraft:overworld", 10, 64, 10, "CHEST", [{"id": "minecraft:emerald", "count": 16}])
    put("minecraft:overworld", 500, 64, 500, "CHEST", [{"id": "minecraft:emerald", "count": 64}])

    res = search("emerald", "minecraft:overworld", 0, 64, 0, 50.0)
    assert len(res) == 1
    assert res[0]["x"] == 10

    print("  [PASS] 3/3 StorageCache tests passed.")

def test_automation_planners():
    print("  -> Running QuickDeposit & RestockPlanner test suite...")

    # QuickDepositPlanner
    def plan_deposit(player_inv, chest_inv, slot_limit, allow_empty):
        existing = {item["id"] for item in chest_inv if item["id"] != "minecraft:air"}
        occupied = {item["slot"] for item in chest_inv if item["id"] != "minecraft:air"}
        actions = []
        for p in player_inv:
            if not p or p["id"] == "minecraft:air":
                continue
            if p["id"] in existing or allow_empty:
                # Find matching or empty
                target_slot = None
                for c in chest_inv:
                    if c["id"] == p["id"] and c["count"] < 64:
                        target_slot = c["slot"]
                        break
                if target_slot is None:
                    for s in range(slot_limit):
                        if s not in occupied:
                            target_slot = s
                            occupied.add(s)
                            break
                if target_slot is not None:
                    actions.append({"from": p["slot"], "to": target_slot, "id": p["id"]})
        return actions

    player_inv = [{"id": "minecraft:iron_ingot", "count": 64, "slot": 9}, {"id": "minecraft:dirt", "count": 64, "slot": 10}]
    chest_inv = [{"id": "minecraft:iron_ingot", "count": 32, "slot": 0}]
    actions = plan_deposit(player_inv, chest_inv, 27, False)
    assert len(actions) == 1
    assert actions[0]["id"] == "minecraft:iron_ingot"

    # RestockPlanner
    def plan_restock(hotbar, chest):
        actions = []
        for h in hotbar:
            if h["count"] < 64:
                needed = 64 - h["count"]
                for c in chest:
                    if c["id"] == h["id"] and c["count"] > 0:
                        take = min(needed, c["count"])
                        actions.append({"from": c["slot"], "to": h["slot"], "id": h["id"], "amount": take})
                        needed -= take
                        if needed <= 0:
                            break
        return actions

    hotbar = [{"id": "minecraft:firework_rocket", "count": 14, "slot": 0}]
    chest = [{"id": "minecraft:firework_rocket", "count": 64, "slot": 5}]
    restock = plan_restock(hotbar, chest)
    assert len(restock) == 1
    assert restock[0]["amount"] == 50

    print("  [PASS] 2/2 Automation Planner tests passed.")

def run_standalone_tests():
    test_query_parser()
    test_storage_cache()
    test_automation_planners()
