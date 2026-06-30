$xmlPath = "app\src\main\res\values\strings.xml"
$content = Get-Content -Path $xmlPath -Raw -Encoding UTF8

# Extract all blocks using regex
$regex = [regex]'(?s)((?:\s*<!--.*?-->)*\s*<string\s+name="([^"]+)".*?>.*?</string>)'
$matches = $regex.Matches($content)

$blocks = @()
foreach ($m in $matches) {
    $obj = New-Object PSObject -Property @{
        Text = $m.Groups[1].Value.Trim()
        Name = $m.Groups[2].Value
    }
    $blocks += $obj
}

$sortedBlocks = $blocks | Sort-Object Name

$newInnerContent = ""
foreach ($b in $sortedBlocks) {
    $newInnerContent += "`n    " + $b.Text + "`n"
}

# Extract the <resources> tag
$resRegex = [regex]'(<resources[^>]*>)'
$resMatch = $resRegex.Match($content)

if ($resMatch.Success) {
    $resourcesTag = $resMatch.Groups[1].Value
    
    $newXml = "<?xml version=`"1.0`" encoding=`"utf-8`"?>`n" + $resourcesTag + "`n" + $newInnerContent + "</resources>`n"
    
    Set-Content -Path $xmlPath -Value $newXml -Encoding UTF8
    Write-Host "Strings sorted successfully."
} else {
    Write-Host "Could not find <resources> tag."
}
