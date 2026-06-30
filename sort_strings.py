import xml.etree.ElementTree as ET
import re

def sort_strings_xml(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    blocks = re.findall(r'((?:\s*<!--.*?-->)*\s*<string\s+name="([^"]+)".*?>.*?</string>)', content, flags=re.DOTALL)
    
    if not blocks:
        print("No strings found.")
        return
        
    sorted_blocks = sorted(blocks, key=lambda x: x[1])
    
    new_inner_content = ""
    for block in sorted_blocks:
        new_inner_content += "\n    " + block[0].strip() + "\n"
        
    resources_match = re.search(r'(<resources[^>]*>)', content)
    if not resources_match:
        print("No resources tag found.")
        return
        
    resources_tag = resources_match.group(1)
    
    new_xml = f"""<?xml version="1.0" encoding="utf-8"?>
{resources_tag}
{new_inner_content}
</resources>
"""
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_xml)
    print("Strings sorted successfully.")

sort_strings_xml('app/src/main/res/values/strings.xml')
