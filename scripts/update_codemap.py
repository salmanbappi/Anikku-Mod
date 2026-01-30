import os
import re

def map_project():
    project_map = "# 🧠 AniZen Project Mind (Auto-Generated Map)\n\n"
    project_map += "## 📂 Module Structure\n"
    
    modules = [d for d in os.listdir('.') if os.path.isdir(d) and not d.startswith('.')]
    for module in sorted(modules):
        project_map += f"- **{module}**: "
        if module == 'app': project_map += "Main UI & ViewModels\n"
        elif module == 'domain': project_map += "Business Logic (Use Cases)\n"
        elif module == 'data': project_map += "Repositories & Data Persistence\n"
        elif module == 'core': project_map += "Cross-module Utilities\n"
        else: project_map += "Support Module\n"

    project_map += "\n## 💎 Core Components\n"
    components = {
        "Player": "app/src/main/java/eu/kanade/tachiyomi/ui/player",
        "Downloader": "app/src/main/java/eu/kanade/tachiyomi/data/download",
        "Trackers": "app/src/main/java/eu/kanade/tachiyomi/data/track",
        "DI (Injekt)": "app/src/main/java/eu/kanade/tachiyomi/di"
    }
    
    for name, path in components.items():
        if os.path.exists(path):
            files = [f for f in os.listdir(path) if f.endswith('.kt')]
            project_map += f"- **{name}**: `{path}`\n"
            project_map += f"  - Key Files: {', '.join(files[:5])}...\n"

    project_map += "\n## 🧪 Recent DI Activity (Injekt Discovery)\n"
    try:
        # Search for Injekt registrations
        registrations = []
        app_module_path = "app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt"
        if os.path.exists(app_module_path):
            with open(app_module_path, 'r') as f:
                content = f.read()
                matches = re.findall(r'addSingleton(?:Factory)?\s*\{?\s*([A-Za-z0-9]+)', content)
                registrations = sorted(list(set(matches)))
        
        project_map += f"Detected {len(registrations)} registered services in AppModule.\n"
        for reg in registrations[:10]:
            project_map += f"- `{reg}`\n"
    except Exception as e:
        project_map += f"DI scanning failed: {str(e)}\n"

    with open('CODE_MAP.md', 'w') as f:
        f.write(project_map)
    print("CODE_MAP.md updated successfully.")

if __name__ == "__main__":
    map_project()

