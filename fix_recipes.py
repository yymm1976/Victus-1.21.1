import json
import glob
import os

recipe_dir = "src/main/resources/data/victus/recipe/"

for f in glob.glob(os.path.join(recipe_dir, "*.json")):
    with open(f, 'r', encoding='utf-8') as file:
        data = json.load(file)
    
    if "key" in data:
        for k, v in data["key"].items():
            if isinstance(v, dict) and "id" in v:
                v["item"] = v.pop("id")
    if "ingredients" in data:
        for ing in data["ingredients"]:
            if isinstance(ing, dict) and "id" in ing:
                ing["item"] = ing.pop("id")
                
    if "result" in data and isinstance(data["result"], dict):
        if "item" in data["result"]:
            data["result"]["id"] = data["result"].pop("item")

    with open(f, 'w', encoding='utf-8') as file:
        json.dump(data, file, indent=2)

print("Fixed recipes")