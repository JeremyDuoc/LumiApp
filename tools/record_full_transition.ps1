$adb = "C:\Users\kiwix\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$serial = "emulator-5554"
$outDir = "visual-qa\evidence\android"

Write-Host "Iniciando navegación guiada hasta el Step 7 en $serial..."

function Tap($x, $y, $delayMs = 600) {
    & $adb -s $serial shell input tap $x $y
    Start-Sleep -Milliseconds $delayMs
}

# 1. Asegurar opción "Seguir mi ciclo" y avanzar (Step 0 -> Step 1)
Tap 720 1250 400
Tap 720 2900 800

# 2. Step 1: Checkbox de consentimiento y continuar (Step 1 -> Step 2)
Tap 180 1600 500
Tap 720 2900 800

# 3. Step 2 -> Step 3 (Omitir apodo)
Tap 720 2900 800

# 4. Step 3: Regularidad (Regular) y continuar (Step 3 -> Step 4)
Tap 720 1080 500
Tap 720 2900 800

# 5. Step 4: No recuerdo y continuar (Step 4 -> Step 5)
Tap 900 950 500
Tap 720 2900 800

# 6. Step 5: Objetivo y continuar (Step 5 -> Step 6)
Tap 400 1100 500
Tap 720 2900 800

# 7. Step 6 -> Step 7 (Terminar -> Resumen)
Tap 720 2900 1000

Write-Host "En Step 7. Iniciando grabación de video en segundo plano..."
$recordJob = Start-Process -FilePath $adb -ArgumentList "-s $serial shell screenrecord --size 720x1280 --bit-rate 4000000 --time-limit 16 /sdcard/full_ceremony.mp4" -PassThru

Start-Sleep -Milliseconds 600

Write-Host "Tocando 'Ver mi inicio' para disparar la transición ceremonial..."
Tap 720 2900 100

# Captura 1: Halo cromático y Mensaje 1
Start-Sleep -Milliseconds 1800
& $adb -s $serial shell screencap -p "/sdcard/ceremony_01_space.png"
& $adb -s $serial pull "/sdcard/ceremony_01_space.png" "$outDir\transition_live_frame_01_glow.png" | Out-Null
Write-Host "Frame 1 capturado."

# Captura 2: Mensaje 2
Start-Sleep -Milliseconds 2200
& $adb -s $serial shell screencap -p "/sdcard/ceremony_02_prefs.png"
& $adb -s $serial pull "/sdcard/ceremony_02_prefs.png" "$outDir\transition_live_frame_02_message.png" | Out-Null
Write-Host "Frame 2 capturado."

# Captura 3: Mensaje 3/4
Start-Sleep -Milliseconds 3200
& $adb -s $serial shell screencap -p "/sdcard/ceremony_03_manner.png"
& $adb -s $serial pull "/sdcard/ceremony_03_manner.png" "$outDir\transition_live_frame_03_contemplation.png" | Out-Null
Write-Host "Frame 3 capturado."

# Captura 4: Vuelo a la cabecera
Start-Sleep -Milliseconds 3600
& $adb -s $serial shell screencap -p "/sdcard/ceremony_04_flight.png"
& $adb -s $serial pull "/sdcard/ceremony_04_flight.png" "$outDir\transition_live_frame_04_flight.png" | Out-Null
Write-Host "Frame 4 capturado."

# Esperar a que screenrecord complete los 16 segundos
Write-Host "Esperando finalización de video..."
Start-Sleep -Seconds 4

& $adb -s $serial pull "/sdcard/full_ceremony.mp4" "$outDir\transition_recording.mp4"
Write-Host "Video transferido con éxito a $outDir\transition_recording.mp4."
