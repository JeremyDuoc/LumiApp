from PIL import Image
from rembg import remove

img = Image.open(r'c:\Users\kiwix\AndroidStudioProjects\Lumi\app\src\main\res\drawable\lumi_logo.png')
# Original bbox is roughly (176, 170, 848, 857)
# Crop bottom to remove 'Lumi' text, roughly removing 150px from bottom.
bbox = img.getbbox()
if bbox:
    cropped = img.crop((bbox[0], bbox[1], bbox[2], bbox[3] - 150))
else:
    cropped = img.crop((176, 170, 848, 700))

out = remove(cropped)
out.save(r'c:\Users\kiwix\AndroidStudioProjects\Lumi\app\src\main\res\drawable\lumi_logo.png')
print("Image processed successfully.")
