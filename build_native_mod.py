#!/usr/bin/env python3
"""
Standalone Native Build Script for VaultFinder Pro
Compiles Java source with JDK 21 javac, executes JUnit test suite,
and packages a production-ready release JAR in build/libs/vaultfinder-pro-1.0.0.jar
with META-INF/MANIFEST.MF and fabric.mod.json.
"""

import os
import sys
import glob
import shutil
import hashlib
import zipfile
import subprocess
from pathlib import Path

def find_jdk_tool(tool_name):
    # 1. Check for JDK 21 installations
    jdk21_patterns = [
        os.path.join(os.environ.get("ProgramFiles", "C:\\Program Files"), "Microsoft", "jdk-21*", "bin", f"{tool_name}.exe"),
        os.path.join(os.environ.get("ProgramFiles", "C:\\Program Files"), "Eclipse Adoptium", "jdk-21*", "bin", f"{tool_name}.exe"),
        os.path.join(os.environ.get("ProgramFiles", "C:\\Program Files"), "Java", "jdk-21*", "bin", f"{tool_name}.exe"),
        os.path.join(os.environ.get("LOCALAPPDATA", ""), "Programs", "**", "jdk-21*", "bin", f"{tool_name}.exe"),
    ]
    for p in jdk21_patterns:
        matches = glob.glob(p, recursive=True)
        if matches:
            return matches[0]

    # 2. Check in PATH
    found = shutil.which(tool_name)
    if found:
        return found
    if sys.platform == 'win32':
        found_exe = shutil.which(f"{tool_name}.exe")
        if found_exe:
            return found_exe

    # 3. Check JAVA_HOME
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = os.path.join(java_home, "bin", f"{tool_name}.exe" if sys.platform == "win32" else tool_name)
        if os.path.isfile(candidate):
            return candidate

    return tool_name

def collect_classpath_jars(root_dir):
    jars = []
    seen = set()

    def add_jar(path):
        norm = os.path.abspath(path)
        if norm not in seen and os.path.isfile(norm) and norm.endswith('.jar'):
            # Filter out corrupt/incomplete files
            if os.path.getsize(norm) > 100:
                seen.add(norm)
                jars.append(norm)

    # 1. Project local loom cache & remapped mods
    local_loom = os.path.join(root_dir, ".gradle", "loom-cache")
    if os.path.isdir(local_loom):
        for f in glob.glob(os.path.join(local_loom, "**", "*.jar"), recursive=True):
            if not f.endswith("-sources.jar"):
                add_jar(f)

    # 2. User gradle fabric-loom cache
    user_home = Path.home()
    loom_cache = os.path.join(str(user_home), ".gradle", "caches", "fabric-loom")
    if os.path.isdir(loom_cache):
        for f in glob.glob(os.path.join(loom_cache, "**", "*.jar"), recursive=True):
            if not f.endswith("-sources.jar"):
                add_jar(f)

    # 3. User gradle modules-2 files (dependencies: fabric-loader, modmenu, junit, etc.)
    modules_cache = os.path.join(str(user_home), ".gradle", "caches", "modules-2", "files-2.1")
    if os.path.isdir(modules_cache):
        for f in glob.glob(os.path.join(modules_cache, "**", "*.jar"), recursive=True):
            if not f.endswith("-sources.jar") and not f.endswith("-javadoc.jar"):
                add_jar(f)

    # 4. Standard .minecraft libraries & versions
    appdata = os.environ.get("APPDATA")
    if appdata:
        mc_dir = os.path.join(appdata, ".minecraft")
        version_1211 = os.path.join(mc_dir, "versions", "1.21.1", "1.21.1.jar")
        if os.path.isfile(version_1211):
            add_jar(version_1211)

        libs_dir = os.path.join(mc_dir, "libraries")
        if os.path.isdir(libs_dir):
            for f in glob.glob(os.path.join(libs_dir, "**", "*.jar"), recursive=True):
                add_jar(f)

        mods_dir = os.path.join(mc_dir, "mods")
        if os.path.isdir(mods_dir):
            for f in glob.glob(os.path.join(mods_dir, "*.jar")):
                if "vaultfinder" not in f.lower() and "modmenu" not in f.lower():
                    add_jar(f)

    return jars

def main():
    root = os.path.abspath(os.path.dirname(__file__))
    os.chdir(root)
    print("================================================================")
    print("  VaultFinder Pro - Standalone JDK 21 Native Build System")
    print("================================================================")

    javac = find_jdk_tool("javac")
    java = find_jdk_tool("java")
    print(f"[*] Found JDK compiler : {javac}")
    print(f"[*] Found Java runtime : {java}")

    # Verify javac version
    try:
        ver_proc = subprocess.run([javac, "-version"], capture_output=True, text=True, check=True)
        ver_out = (ver_proc.stdout + ver_proc.stderr).strip()
        print(f"[*] Compiler version   : {ver_out}")
    except Exception as e:
        print(f"[!] Warning: Could not verify javac version: {e}")

    # Collect dependencies
    print("[*] Resolving classpath dependencies from Gradle & Minecraft caches...")
    cp_jars = collect_classpath_jars(root)
    print(f"[*] Resolved {len(cp_jars)} classpath JARs.")

    tmp_dir = os.path.join(root, "tmp_build")
    os.makedirs(tmp_dir, exist_ok=True)

    # 1. Collect main Java sources
    src_main_dir = os.path.join(root, "src", "main", "java")
    main_sources = []
    for r, _, files in os.walk(src_main_dir):
        for f in files:
            if f.endswith(".java"):
                main_sources.append(os.path.join(r, f).replace("\\", "/"))

    print(f"[*] Found {len(main_sources)} main Java source files.")

    main_classes_dir = os.path.join(root, "build", "classes", "java", "main")
    os.makedirs(main_classes_dir, exist_ok=True)

    # Write cp argument file
    main_cp_str = os.pathsep.join(cp_jars).replace("\\", "/")
    main_cp_file = os.path.join(tmp_dir, "main_cp.txt")
    with open(main_cp_file, "w", encoding="utf-8") as f:
        f.write("-cp\n\"" + main_cp_str + "\"\n")

    main_sources_file = os.path.join(tmp_dir, "main_sources.txt")
    with open(main_sources_file, "w", encoding="utf-8") as f:
        f.write("\n".join(f'"{s}"' for s in main_sources) + "\n")

    # Compile main sources
    print("[*] Compiling main Java classes with javac (release 21)...")
    compile_cmd = [
        javac,
        "-proc:none",
        f"@{main_cp_file}",
        f"@{main_sources_file}",
        "-d", main_classes_dir,
        "-encoding", "UTF-8",
        "--release", "21"
    ]

    res = subprocess.run(compile_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print("[!] COMPILATION FAILED:")
        print(res.stderr)
        sys.exit(1)
    print("[+] Main classes compiled successfully.")

    # 2. Compile tests if present
    src_test_dir = os.path.join(root, "src", "test", "java")
    test_sources = []
    if os.path.isdir(src_test_dir):
        for r, _, files in os.walk(src_test_dir):
            for f in files:
                if f.endswith(".java"):
                    test_sources.append(os.path.join(r, f).replace("\\", "/"))

    test_classes_dir = os.path.join(root, "build", "classes", "java", "test")
    os.makedirs(test_classes_dir, exist_ok=True)

    if test_sources:
        print(f"[*] Found {len(test_sources)} test Java source files.")
        test_cp_jars = [main_classes_dir] + cp_jars
        test_cp_str = os.pathsep.join(test_cp_jars).replace("\\", "/")
        test_cp_file = os.path.join(tmp_dir, "test_cp.txt")
        with open(test_cp_file, "w", encoding="utf-8") as f:
            f.write("-cp\n\"" + test_cp_str + "\"\n")

        test_sources_file = os.path.join(tmp_dir, "test_sources.txt")
        with open(test_sources_file, "w", encoding="utf-8") as f:
            f.write("\n".join(f'"{s}"' for s in test_sources) + "\n")

        print("[*] Compiling test Java classes...")
        test_compile_cmd = [
            javac,
            "-proc:none",
            f"@{test_cp_file}",
            f"@{test_sources_file}",
            "-d", test_classes_dir,
            "-encoding", "UTF-8",
            "--release", "21"
        ]
        res_test = subprocess.run(test_compile_cmd, capture_output=True, text=True)
        if res_test.returncode != 0:
            print("[!] TEST COMPILATION FAILED:")
            print(res_test.stderr)
            sys.exit(1)
        print("[+] Test classes compiled successfully.")

        # Run tests with TestRunner if present
        print("[*] Running JUnit tests...")
        run_cp_jars = [test_classes_dir, main_classes_dir] + cp_jars
        run_cp_str = os.pathsep.join(run_cp_jars).replace("\\", "/")
        run_cp_file = os.path.join(tmp_dir, "run_cp.txt")
        with open(run_cp_file, "w", encoding="utf-8") as f:
            f.write("-cp\n\"" + run_cp_str + "\"\n")

        test_run_cmd = [
            java,
            f"@{run_cp_file}",
            "com.slopgobbler.vaultfinder.TestRunner"
        ]
        test_res = subprocess.run(test_run_cmd, capture_output=True, text=True)
        print(test_res.stdout)
        if test_res.stderr:
            print(test_res.stderr)
        if test_res.returncode != 0:
            print("[!] JUnit test suite failed!")
            sys.exit(1)

    # 3. Package release JAR
    output_jar = os.path.join(root, "build", "libs", "vaultfinder-pro-1.0.0.jar")
    os.makedirs(os.path.dirname(output_jar), exist_ok=True)
    print(f"[*] Packaging release JAR: {output_jar}")

    manifest_content = (
        "Manifest-Version: 1.0\r\n"
        "Fabric-Loom-Version: 1.9.2\r\n"
        "Implementation-Title: VaultFinder Pro\r\n"
        "Implementation-Version: 1.0.0\r\n"
        "Implementation-Vendor: slopgobbler\r\n"
        "Specification-Title: VaultFinder Pro\r\n"
        "Specification-Version: 1.0.0\r\n"
        "Specification-Vendor: slopgobbler\r\n"
        "\r\n"
    )

    with zipfile.ZipFile(output_jar, "w", zipfile.ZIP_DEFLATED) as zf:
        # Write MANIFEST.MF
        zf.writestr("META-INF/MANIFEST.MF", manifest_content.encode("utf-8"))

        # Add all compiled main classes
        for r, _, files in os.walk(main_classes_dir):
            for file in files:
                full_path = os.path.join(r, file)
                rel_path = os.path.relpath(full_path, main_classes_dir).replace("\\", "/")
                zf.write(full_path, rel_path)

        # Add resources
        res_dir = os.path.join(root, "src", "main", "resources")
        if os.path.isdir(res_dir):
            for r, _, files in os.walk(res_dir):
                for file in files:
                    full_path = os.path.join(r, file)
                    rel_path = os.path.relpath(full_path, res_dir).replace("\\", "/")
                    zf.write(full_path, rel_path)

        # Add LICENSE if present
        license_path = os.path.join(root, "LICENSE")
        if os.path.isfile(license_path):
            zf.write(license_path, "LICENSE_vaultfinder")

    # Compute SHA-256 Checksum
    with open(output_jar, "rb") as f:
        jar_bytes = f.read()
        sha256 = hashlib.sha256(jar_bytes).hexdigest()
        size_kb = len(jar_bytes) / 1024.0

    checksums_file = os.path.join(root, "build", "libs", "CHECKSUMS.txt")
    with open(checksums_file, "w", encoding="utf-8") as f:
        f.write(f"{sha256}  vaultfinder-pro-1.0.0.jar\n")

    print("================================================================")
    print("  BUILD SUCCESSFUL")
    print("================================================================")
    print(f"  Artifact : {output_jar}")
    print(f"  Size     : {size_kb:.2f} KB ({len(jar_bytes)} bytes)")
    print(f"  SHA-256  : {sha256}")
    print(f"  Checksums: {checksums_file}")
    print("================================================================")
    return True

if __name__ == "__main__":
    main()
