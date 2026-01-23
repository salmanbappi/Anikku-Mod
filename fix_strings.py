import os
import re

def fix_placeholders(match):
    tag_start = match.group(1)
    content = match.group(2)
    tag_end = match.group(3)
    
    # Find all placeholders like %d, %s, %.2f, %02d, %c, etc.
    # But NOT %%
    placeholders = re.findall(r'%(?![%])(?:[0-9]+\$)?(?:[-+ #0])?(?:\d+)?(?:\.\d+)?[diouxXeEfFgGcrs]', content)
    
    if len(placeholders) <= 1:
        return match.group(0)
    
    # Check if they are already positional
    # If all are already positional, return original
    if all('$' in p for p in placeholders):
        return match.group(0)

    count = 1
    # This regex matches placeholders that don't have $ in them
    def replacer(m):
        nonlocal count
        p = m.group(0)
        # Insert positional marker
        # p[0] is '%'
        new_p = f'%{count}${p[1:]}'
        count += 1
        return new_p

    # This regex matches placeholders that don't have $ in them
    new_content = re.sub(r'%(?![%])(?![0-9]+\$)(?:[-+ #0])?(?:\d+)?(?:\.\d+)?[diouxXeEfFgGcrs]', replacer, content)
    
    return f'{tag_start}{new_content}{tag_end}'

def process_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Match <string ... name="...">...</string>
    # OR <item ...>...</item> inside <plurals>
    if file_path.endswith('strings.xml'):
        new_content = re.sub(r'(<string\b[^>]*\bname="[^"]+"[^>]*>)(.*?)(</string>)', fix_placeholders, content, flags=re.DOTALL)
    elif file_path.endswith('plurals.xml'):
        new_content = re.sub(r'(<item\b[^>]*>)(.*?)(</item>)', fix_placeholders, content, flags=re.DOTALL)
    else:
        return False

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def main():
    root_dir = 'i18n/src/commonMain/moko-resources'
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file in ['strings.xml', 'plurals.xml']:
                path = os.path.join(root, file)
                if process_file(path):
                    print(f'Fixed: {path}')

if __name__ == "__main__":
    main()
