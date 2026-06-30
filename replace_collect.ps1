$dirPath = "c:\Users\kiwix\AndroidStudioProjects\Lumi\app\src\main\java\com\jeremy\lumi"
$importOld = "import androidx.compose.runtime.collectAsState"
$importNew = "import androidx.lifecycle.compose.collectAsStateWithLifecycle"

Get-ChildItem -Path $dirPath -Filter "*.kt" -Recurse | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file -Raw
    
    if ($content -match "\.collectAsState\(") {
        $newContent = $content -replace "\.collectAsState\(", ".collectAsStateWithLifecycle("
        
        if ($newContent -match "import androidx.compose.runtime.collectAsState") {
            $newContent = $newContent -replace "import androidx.compose.runtime.collectAsState", $importNew
        } elseif ($newContent -notmatch "import androidx.compose.runtime.\*") {
            if ($newContent -notmatch "import androidx.lifecycle.compose.collectAsStateWithLifecycle") {
                $newContent = $newContent -replace "(?m)^(package .*?\r?\n)", "`$1`r`n$importNew"
            }
        }
        
        Set-Content -Path $file -Value $newContent -Encoding UTF8
        Write-Host "Replaced in $file"
    }
}
