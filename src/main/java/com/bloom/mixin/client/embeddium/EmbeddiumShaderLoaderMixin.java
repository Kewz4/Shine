package com.bloom.mixin.client.embeddium;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects bloom output code into Embeddium's opaque block layer fragment shader.
 * Adds a second fragment output (bloomColor) that writes per-pixel bloom strength
 * data to MRT attachment 1 during terrain rendering.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class EmbeddiumShaderLoaderMixin {

    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, require = 0)
    private static void shine$injectBloomOutput(ResourceLocation name, CallbackInfoReturnable<String> cir) {
        String shader = cir.getReturnValue();
        if (shader == null) return;

        // Only target the opaque block layer fragment shader
        String path = name.getPath();
        if (!path.contains("block_layer_opaque") || !path.endsWith(".fsh")) return;
        if (shader.contains("bloomColor")) return; // already injected

        // Add bloom output declaration after the fragColor declaration
        String injected = shader;

        // Step 1: add second output
        injected = injected.replace(
            "out vec4 fragColor;",
            "out vec4 fragColor;\nlayout(location = 1) out vec4 bloomColor;"
        );
        if (injected.equals(shader)) {
            // Fallback without layout qualifier (older GLSL)
            injected = shader.replace(
                "out vec4 fragColor",
                "out vec4 fragColor;\nout vec4 bloomColor"
            );
        }
        if (injected.equals(shader)) return; // couldn't find output declaration

        // Step 2: inject bloom decode helper before main()
        injected = injected.replace(
            "void main() {",
            """
            float shine_decode_source_strength(uint packedMaterial) {
                uint code = (packedMaterial >> 3u) & 0x1Fu;
                if (code <= 20u) return float(code) * 0.05;
                return 1.0 + float(code - 20u) * (4.0 / 11.0);
            }

            void main() {
            """
        );

        // Step 3: inject bloom output write at end of main
        // Try to find common fog function patterns used in Embeddium shaders
        String[] fogEndPatterns = {
            "    fragColor = _linearFog(color, v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor);\n}",
            "    fragColor = linear_fog(color",
            "fragColor = u_FogColor"
        };

        boolean injectedEnd = false;
        for (String pattern : fogEndPatterns) {
            if (injected.contains(pattern.split("\n")[0])) {
                // Find the closing brace after the fragColor assignment
                int idx = injected.lastIndexOf("fragColor =");
                int closingBrace = injected.indexOf('\n', idx);
                while (closingBrace < injected.length() - 1 &&
                       injected.charAt(closingBrace + 1) == '\n') closingBrace++;
                // Insert bloom output before closing brace
                int endBrace = injected.lastIndexOf('}');
                if (endBrace > idx) {
                    String before = injected.substring(0, endBrace);
                    String after = injected.substring(endBrace);
                    injected = before +
                        """
                            // Shine bloom output
                            if (v_Material == 0u) {
                                bloomColor = vec4(0.0);
                            } else {
                                float bloomStrength = shine_decode_source_strength(v_Material);
                                if (bloomStrength <= 1.0e-5) {
                                    bloomColor = vec4(0.0);
                                } else {
                                    bloomColor = vec4(fragColor.rgb * fragColor.a, clamp(bloomStrength / 5.0, 0.0, 1.0));
                                }
                            }
                        """ + after;
                    injectedEnd = true;
                    break;
                }
            }
        }

        if (!injectedEnd) {
            // Simple fallback: append before closing brace
            int endBrace = injected.lastIndexOf('}');
            if (endBrace > 0) {
                injected = injected.substring(0, endBrace) +
                    """
                        bloomColor = vec4(0.0);
                    """ + injected.substring(endBrace);
            }
        }

        cir.setReturnValue(injected);
    }
}
