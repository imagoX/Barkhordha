const canvas = document.getElementById('glitchCanvas');
const ctx = canvas.getContext('2d');
const imageInput = document.getElementById('imageInput');
const NUM_SLICES = 15; // Number of vertical slices

imageInput.addEventListener('change', function (event) {
    const file = event.target.files[0];
    if (!file) return;

    const img = new Image();
    img.src = URL.createObjectURL(file);
    img.onload = function () {
        processImage(img);
    };
});

function processImage(image) {
    const size = Math.min(image.width, image.height); // Ensure 1:1 crop
    canvas.width = size;
    canvas.height = size;

    // Crop the image into a 1:1 ratio
    ctx.drawImage(image, (image.width - size) / 2, (image.height - size) / 2, size, size, 0, 0, size, size);

    applyGlitchEffect();
}

function applyGlitchEffect() {
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const pixels = imageData.data;

    for (let i = 0; i < pixels.length; i += 4) {
        const r = pixels[i];
        const g = pixels[i + 1];
        const b = pixels[i + 2];

        // Convert to grayscale (Black & White)
        const avg = (r + g + b) / 3;
        pixels[i] = pixels[i + 1] = pixels[i + 2] = avg;

        // Apply glitch effect: detect edges and add color
        if (avg < 50) { // Darker areas get colorized borders
            pixels[i] = 255;  // Red
            pixels[i + 1] = 255; // Green
            pixels[i + 2] = 0;   // Yellow
        }
    }

    ctx.putImageData(imageData, 0, 0);
}
