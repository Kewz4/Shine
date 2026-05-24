#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomLevel0;
uniform sampler2D BloomLevel1;
uniform sampler2D BloomLevel2;
uniform sampler2D BloomLevel3;
uniform sampler2D BloomLevel4;
uniform sampler2D BloomLevel5;
uniform vec2 OutSize;
uniform float Strength;
uniform float Weight0;
uniform float Weight1;
uniform float Weight2;
uniform float Weight3;
uniform float Weight4;
uniform float Weight5;

in vec2 texCoord;
out vec4 fragColor;

vec3 sample_smooth(sampler2D s, vec2 uv) {
    return texture(s, uv).rgb;
}

void main() {
    vec2 uv = gl_FragCoord.xy / OutSize;
    vec4 scene = texture(DiffuseSampler, uv);
    vec3 bloom = vec3(0.0);
    bloom += sample_smooth(BloomLevel0, uv) * Weight0;
    bloom += sample_smooth(BloomLevel1, uv) * Weight1;
    bloom += sample_smooth(BloomLevel2, uv) * Weight2;
    bloom += sample_smooth(BloomLevel3, uv) * Weight3;
    bloom += sample_smooth(BloomLevel4, uv) * Weight4;
    bloom += sample_smooth(BloomLevel5, uv) * Weight5;
    bloom *= Strength;
    fragColor = vec4(scene.rgb + bloom, scene.a);
}
