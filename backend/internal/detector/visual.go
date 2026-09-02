package detector

import (
	"bytes"
	"fmt"
	"image"
	"image/color"
	_ "image/jpeg"
)

// FrameDifference returns the proportion of sampled pixels whose luminance
// changed materially. Sampling every eighth pixel keeps the fallback cheap.
func FrameDifference(before, after []byte) (float64, error) {
	a, _, err := image.Decode(bytes.NewReader(before))
	if err != nil {
		return 0, fmt.Errorf("decode previous frame: %w", err)
	}
	b, _, err := image.Decode(bytes.NewReader(after))
	if err != nil {
		return 0, fmt.Errorf("decode current frame: %w", err)
	}
	bounds := a.Bounds().Intersect(b.Bounds())
	if bounds.Empty() {
		return 0, fmt.Errorf("frames do not overlap")
	}
	var changed, samples int
	for y := bounds.Min.Y; y < bounds.Max.Y; y += 8 {
		for x := bounds.Min.X; x < bounds.Max.X; x += 8 {
			aY := luminance(a.At(x, y))
			bY := luminance(b.At(x, y))
			if abs(aY-bY) > 24 {
				changed++
			}
			samples++
		}
	}
	if samples == 0 {
		return 0, nil
	}
	return float64(changed) / float64(samples), nil
}

func luminance(c color.Color) int {
	r, g, b, _ := c.RGBA()
	return int((299*r + 587*g + 114*b) / 1000 >> 8)
}

func abs(value int) int {
	if value < 0 {
		return -value
	}
	return value
}
