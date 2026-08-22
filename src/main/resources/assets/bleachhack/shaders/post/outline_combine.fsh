#version 330

// BleachHack entity highlight, pass 2 of 2.
//
// Finish the distance field along Y, then draw:
//   inside the silhouette -> translucent fill
//   within Radius of it   -> the outline, shaded across its width by Style
//   anything else         -> nothing
//
// Style: 0 solid, 1 inline (dark band hugging the entity, colour outside),
//        2 gradient (dark to colour across the whole width).

uniform sampler2D InSampler;
uniform sampler2D OrigSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
    vec2 OrigSize;
};

layout(std140) uniform BleachOutline {
    float Fill;
    float Radius;
    float Style;
};

const float DIST_SCALE = 8.0;
const float INLINE_WIDTH = 0.45;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float texel = 1.0 / InSize.y;
    int radius = int(max(Radius, 1.0));

    float bestDistance = DIST_SCALE;
    vec3 bestColor = vec3(0.0);

    for (int j = -radius; j <= radius; j++) {
        vec4 column = texture(InSampler, texCoord + vec2(0.0, float(j) * texel));

        // alpha 1.0 means pass 1 found nothing in that column
        if (column.a < 1.0) {
            float dx = column.a * DIST_SCALE;
            float distance = sqrt(dx * dx + float(j * j));

            if (distance < bestDistance) {
                bestDistance = distance;
                bestColor = column.rgb;
            }
        }
    }

    vec4 original = texture(OrigSampler, texCoord);

    if (original.a > 0.0) {
        fragColor = vec4(original.rgb, original.a * Fill);
        return;
    }

    if (bestDistance > float(radius)) {
        fragColor = vec4(0.0);
        return;
    }

    // 0 right against the silhouette, 1 at the outer edge of the outline
    float t = bestDistance / float(radius);
    vec3 color;

    if (Style < 0.5) {
        color = bestColor;
    } else if (Style < 1.5) {
        color = t < INLINE_WIDTH ? vec3(0.0) : bestColor;
    } else {
        color = mix(vec3(0.0), bestColor, t);
    }

    fragColor = vec4(color, 1.0);
}
