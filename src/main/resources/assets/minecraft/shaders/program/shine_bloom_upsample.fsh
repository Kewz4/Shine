#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D LargerSampler;
uniform vec2 OutSize;
uniform float SmallWeight;
uniform float LargeWeight;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 invSize = 1.0 / OutSize;
    vec3 small = vec3(0.0);
    small += texture(DiffuseSampler, texCoord + invSize * vec2( 1.0,  1.0)).rgb;
    small += texture(DiffuseSampler, texCoord + invSize * vec2(-1.0,  1.0)).rgb * 2.0;
    small += texture(DiffuseSampler, texCoord + invSize * vec2( 0.0,  1.0)).rgb;
    small += texture(DiffuseSampler, texCoord + invSize * vec2( 1.0,  0.0)).rgb * 2.0;
    small += texture(DiffuseSampler, texCoord).rgb * 4.0;
    small += texture(DiffuseSampler, texCoord + invSize * vec2(-1.0,  0.0)).rgb * 2.0;
    small += texture(DiffuseSampler, texCoord + invSize * vec2( 1.0, -1.0)).rgb;
    small += texture(DiffuseSampler, texCoord + invSize * vec2(-1.0, -1.0)).rgb * 2.0;
    small += texture(DiffuseSampler, texCoord + invSize * vec2( 0.0, -1.0)).rgb;
    small /= 16.0;

    vec3 large = texture(LargerSampler, texCoord).rgb;
    fragColor = vec4(small * SmallWeight + large * LargeWeight, 1.0);
}
