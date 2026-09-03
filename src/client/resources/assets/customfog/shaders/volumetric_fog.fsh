#version 330

uniform sampler2D DepthSampler;

layout(std140) uniform FogVolume {
    mat4 InvProjView;
    vec4 FogColor;
    vec4 CameraPosAndTime;
    vec4 Params;
    vec4 Params2;
};

in vec2 texCoord;
out vec4 fragColor;

float hash12(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

vec3 reconstructWorld(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth, 1.0);
    vec4 world = InvProjView * clip;
    return world.xyz / max(world.w, 1e-6);
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    vec3 camera = CameraPosAndTime.xyz;
    float time = CameraPosAndTime.w;
    float density = Params.x;
    float inWater = Params.y;
    float seaLevel = Params.z;
    float noiseScale = Params.w;
    float noiseStrength = Params2.x;
    float farPlane = Params2.z;

    vec3 endPos = reconstructWorld(texCoord, depth);
    vec3 ray = endPos - camera;
    float dist = min(length(ray), farPlane);
    if (dist < 0.05 || dist != dist) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 dir = ray / max(length(ray), 1e-4);
    float stepLen = dist / 12.0;
    vec3 pos = camera + dir * (0.4 + hash12(texCoord * 40.0) * 0.6) * stepLen;
    float optical = 0.0;

    for (int i = 0; i < 12; i++) {
        float worldT = smoothstep(1000.0, 5000.0, length(pos.xz));
        worldT = worldT * worldT * (3.0 - 2.0 * worldT);
        float n = mix(1.0, noise2(pos.xz * noiseScale + vec2(time * 0.08, time * 0.05)), noiseStrength);
        float local;
        if (inWater > 0.5) {
            float depthT = smoothstep(0.0, 22.0, max(seaLevel - pos.y, 0.0));
            float originMul = mix(0.55, 2.9, worldT);
            float depthMul = mix(0.5, 1.85, depthT);
            local = density * originMul * depthMul * n;
        } else {
            local = density * mix(0.0, 2.4, worldT) * n;
        }
        optical += local * stepLen;
        if (optical > 5.0) {
            break;
        }
        pos += dir * stepLen;
    }

    float fogAmount = 1.0 - exp(-optical);
    fragColor = vec4(FogColor.rgb, fogAmount);
}
