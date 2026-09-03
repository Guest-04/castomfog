package dev.customfog.client.iris;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mojang.blaze3d.textures.GpuTexture;

import net.fabricmc.loader.api.FabricLoader;

public final class IrisCompat {
	private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
	private static Object api;

	private IrisCompat() {
	}

	public static boolean shaderPackInUse() {
		Object iris = api();
		if (iris == null) {
			return false;
		}

		try {
			Object inUse = iris.getClass().getMethod("isShaderPackInUse").invoke(iris);
			return inUse instanceof Boolean flag && flag;
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}

	public static boolean renderingShadowPass() {
		Object iris = api();
		if (iris == null) {
			return false;
		}

		try {
			Object shadow = iris.getClass().getMethod("isRenderingShadowPass").invoke(iris);
			return shadow instanceof Boolean flag && flag;
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}

	public static GpuTexture worldDepthTexture() {
		if (!shaderPackInUse()) {
			return null;
		}

		try {
			Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
			Object manager = iris.getMethod("getPipelineManager").invoke(null);
			Object pipeline = invoke(manager, "getPipelineNullable");
			if (pipeline == null) {
				Object optional = invoke(manager, "getPipeline");
				if (optional instanceof java.util.Optional<?> opt) {
					pipeline = opt.orElse(null);
				}
			}
			if (pipeline == null) {
				return null;
			}

			Object targets = invoke(pipeline, "getRenderTargets");
			if (targets == null) {
				targets = field(pipeline, "renderTargets");
			}
			if (targets == null) {
				return null;
			}

			Object depth = invoke(targets, "getDepthTexture");
			return depth instanceof GpuTexture texture && !texture.isClosed() ? texture : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private static Object api() {
		if (!IRIS_LOADED) {
			return null;
		}
		if (api != null) {
			return api;
		}

		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			api = apiClass.getMethod("getInstance").invoke(null);
			return api;
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	private static Object invoke(Object target, String name) throws ReflectiveOperationException {
		if (target == null) {
			return null;
		}
		try {
			Method method = target.getClass().getMethod(name);
			method.setAccessible(true);
			return method.invoke(target);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static Object field(Object target, String name) throws ReflectiveOperationException {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(target);
			} catch (NoSuchFieldException ignored) {
			}
		}
		return null;
	}
}
