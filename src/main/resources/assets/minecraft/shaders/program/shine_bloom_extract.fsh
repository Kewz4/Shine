#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform float Threshold;
uniform float HighlightClamp;
uniform float SoftKnee;
uniform float MaxDistance;
uniform float NearPlane;
uniform float FarPlane;
uniform float SourceStrengthScale;
uniform float DistanceFadeRange;

in vec2 texCoord;
out vec4 fragColor;

float linearize_depth(float depth) {
    float zNdc = depth * 2.0 - 1.0;
    float denom = FarPlane + NearPlane - zNdc * (FarPlane - NearPlane);
    return (2.0 * NearPlane * FarPlane) / max(denom, 1.0e-6);
}

float distance_limit(float depth, float dist) {
    if (depth >= 0.9999) return 1.0;
    return 1.0 - smoothstep(MaxDistance, MaxDistance + DistanceFadeRange, dist);
}

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float terrainDepth = texture(DepthSampler, texCoord).r;
    float terrainDist = linearize_depth(terrainDepth);

    float encodedStrength = clamp(source.a, 0.0, 1.0);
    if (encodedStrength <= 1.0e-5) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 rawColor = source.rgb;
    float rawBrightness = max(max(rawColor.r, rawColor.g), rawColor.b);
    if (rawBrightness <= 1.0e-6) {
        fragColor = vec4(0.0);
        return;
    }

    float clampedBrightness = min(rawBrightness, HighlightClamp);
    float bloomMask = smoothstep(Threshold, Threshold + max(SoftKnee, 1.0e-4), clampedBrightness);
    float distMask = distance_limit(terrainDepth, terrainDist);
    float highlightScale = clampedBrightness / rawBrightness;
    float sourceStrength = encodedStrength * SourceStrengthScale;
    vec3 bloomColor = rawColor * highlightScale * bloomMask * sourceStrength * distMask;
    fragColor = vec4(bloomColor, 1.0);
}
