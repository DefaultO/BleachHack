#version 330

// BleachHack entity highlight.
//
// The entity_outline target holds a flat-colored silhouette of every entity that
// was given a non-zero EntityRenderState.outlineColor. We turn that into the
// classic BleachHack look: a solid 1px rim around the silhouette plus a
// translucent interior fill.
//
// The fill opacity rides in the silhouette's own alpha channel (ESP writes it as
// the alpha of outlineColor), because a PostPass bakes its uniforms at build time
// and can't be updated per frame. Encoding it per entity in the color also means
// every entity can have its own fill strength.

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 oneTexel = 1.0 / InSize;

    vec4 center = texture(InSampler, texCoord);
    vec4 left   = texture(InSampler, texCoord - vec2(oneTexel.x, 0.0));
    vec4 right  = texture(InSampler, texCoord + vec2(oneTexel.x, 0.0));
    vec4 up     = texture(InSampler, texCoord - vec2(0.0, oneTexel.y));
    vec4 down   = texture(InSampler, texCoord + vec2(0.0, oneTexel.y));

    // Any alpha discontinuity against a neighbour means we're on the silhouette edge.
    float edge = abs(center.a - left.a)
               + abs(center.a - right.a)
               + abs(center.a - up.a)
               + abs(center.a - down.a);

    // Take the strongest neighbouring color so the rim keeps the entity's color
    // even on the transparent side of the edge.
    vec3 rgb = max(max(max(max(center.rgb, left.rgb), right.rgb), up.rgb), down.rgb);

    // Edge -> solid outline, interior -> whatever fill alpha the entity asked for.
    float alpha = edge > 0.0 ? 1.0 : center.a;

    fragColor = vec4(rgb, alpha);
}
