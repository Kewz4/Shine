#version 150

in vec4 Position;

uniform vec2 OutSize;

out vec2 texCoord;

void main() {
    vec2 ndc;
    ndc.x = (Position.x > 0.5) ? 1.0 : -1.0;
    ndc.y = (Position.y > 0.5) ? 1.0 : -1.0;
    gl_Position = vec4(ndc, 0.2, 1.0);
    texCoord = Position.xy / OutSize;
}
