import os
import re

dir_path = r'c:\Users\kiwix\AndroidStudioProjects\Lumi\app\src\main\java\com\jeremy\lumi'

import_old = 'import androidx.compose.runtime.collectAsState'
import_new = 'import androidx.lifecycle.compose.collectAsStateWithLifecycle'

for root, dirs, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            if '.collectAsState(' in content or '.collectAsState()' in content:
                # Replace method calls
                new_content = content.replace('.collectAsState(', '.collectAsStateWithLifecycle(')
                
                # Replace imports
                if import_old in new_content:
                    new_content = new_content.replace(import_old, import_new)
                elif 'import androidx.compose.runtime.*' not in new_content:
                    # sometimes collectAsState is imported via *, if not, let's explicitly add our import
                    if import_new not in new_content:
                        # insert import after package
                        package_match = re.search(r'package\s+[a-zA-Z0-9_\.]+', new_content)
                        if package_match:
                            new_content = new_content[:package_match.end()] + '\n\n' + import_new + new_content[package_match.end():]

                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f'Replaced in {filepath}')
