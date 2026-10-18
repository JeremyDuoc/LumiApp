$adb = "C:\Users\kiwix\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$outDir = "visual-qa\evidence\android"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Capture($name) {
    Start-Sleep -Milliseconds 500
    & $adb shell screencap -p "/sdcard/$name.png"
    & $adb pull "/sdcard/$name.png" "$outDir\$name.png" | Out-Null
    Write-Host "[Captured Android] -> $name.png"
}

# 1. Step 1: Aceptar consentimiento
Write-Host "Paso 1: Aceptando consentimiento..."
& $adb shell input tap 180 1600
Start-Sleep -Milliseconds 600
Capture "step_1_accepted"

# Avanzar a Paso 2
& $adb shell input tap 720 2880
Start-Sleep -Milliseconds 800
Capture "step_2"

# 2. Paso 2: Escribir apodo Valentina
Write-Host "Paso 2: Escribiendo apodo Valentina..."
& $adb shell input tap 720 1050
Start-Sleep -Milliseconds 400
& $adb shell input text "Valentina"
Start-Sleep -Milliseconds 400
& $adb shell input keyevent 111 # Escape / cerrar teclado
Start-Sleep -Milliseconds 400
Capture "step_2_filled"

# Avanzar a Paso 3
& $adb shell input tap 720 2880
Start-Sleep -Milliseconds 800
Capture "step_3"

# 3. Paso 3: Regularidad de ciclo
Write-Host "Paso 3: Seleccionando regularidad..."
& $adb shell input tap 720 1080
Start-Sleep -Milliseconds 400
Capture "step_3_selected"

# Avanzar a Paso 4
& $adb shell input tap 720 2880
Start-Sleep -Milliseconds 800
Capture "step_4"

# 4. Paso 4: Último periodo (No recuerdo)
Write-Host "Paso 4: Seleccionando No recuerdo..."
& $adb shell input tap 900 950
Start-Sleep -Milliseconds 400
Capture "step_4_no_date"

# Avanzar a Paso 5
& $adb shell input tap 720 2880
Start-Sleep -Milliseconds 800
Capture "step_5"

# 5. Paso 5: Preferencias y Anticoncepción
Write-Host "Paso 5: Seleccionando objetivo y glider hormonal..."
& $adb shell input tap 400 1100
Start-Sleep -Milliseconds 300
& $adb shell input tap 300 2150
Start-Sleep -Milliseconds 400
Capture "step_5_selected"

# Avanzar a Paso 6
& $adb shell input tap 720 2880
Start-Sleep -Milliseconds 800
Capture "step_6"

# 6. Paso 6: Personalización
Write-Host "Paso 6: Seleccionando tile Cólicos..."
& $adb shell input tap 250 1850
Start-Sleep -Milliseconds 400
Capture "step_6_selected"

# Avanzar a Paso 7
& $adb shell input tap 720 2880
Start-Sleep -Milliseconds 800
Capture "step_7_summary"

# 7. Theme Sheet: Capturar modal y temas
Write-Host "Abriendo Theme Sheet..."
& $adb shell input tap 1200 240
Start-Sleep -Milliseconds 600
Capture "theme_sheet_modal"

Write-Host "Cambiando a tema Ciruela..."
& $adb shell input tap 400 1850
Start-Sleep -Milliseconds 600
Capture "theme_ciruela"

Write-Host "Abriendo Theme Sheet y cambiando a tema Bosque..."
& $adb shell input tap 1200 240
Start-Sleep -Milliseconds 600
& $adb shell input tap 1040 1850
Start-Sleep -Milliseconds 600
Capture "theme_bosque"

Write-Host "Abriendo Theme Sheet y cambiando a tema Pétalo (claro)..."
& $adb shell input tap 1200 240
Start-Sleep -Milliseconds 600
& $adb shell input tap 400 2450
Start-Sleep -Milliseconds 600
Capture "theme_petalo"

Write-Host "Regresando a tema Noir..."
& $adb shell input tap 1200 240
Start-Sleep -Milliseconds 600
& $adb shell input tap 400 1550
Start-Sleep -Milliseconds 600
Capture "theme_noir_returned"

# 8. GRABACIÓN DE VIDEO DE LA TRANSICIÓN ATMOSFÉRICA
Write-Host "Iniciando grabación de video de la transición atmosférica (~15s)..."
$recordJob = Start-Job -ScriptBlock {
    param($adbPath)
    & $adbPath shell screenrecord --time-limit 15 /sdcard/transition.mp4
} -ArgumentList $adb

Start-Sleep -Seconds 1
# Tocar "Ver mi inicio"
Write-Host "Tocando 'Ver mi inicio' para disparar la ceremonia atmosférica..."
& $adb shell input tap 720 2880

# Capturar fotogramas en vivo de la transición
Start-Sleep -Seconds 2
Capture "transition_live_frame_01_glow"
Start-Sleep -Seconds 3
Capture "transition_live_frame_02_message"
Start-Sleep -Seconds 4
Capture "transition_live_frame_03_contemplation"
Start-Sleep -Seconds 3
Capture "transition_live_frame_04_flight"

# Esperar a que la grabación termine
Wait-Job $recordJob | Out-Null
Receive-Job $recordJob | Out-Null
Remove-Job $recordJob

Start-Sleep -Seconds 1
& $adb pull /sdcard/transition.mp4 "$outDir\transition_recording.mp4"
Write-Host "✅ Grabación completada y guardada en $outDir\transition_recording.mp4"
