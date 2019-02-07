/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright 2017 Dr.-Ing. Marc Mültin (V2G Clarity)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package com.v2gclarity.risev2g.shared.misc;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import com.v2gclarity.risev2g.shared.utils.MiscUtils;

/**
 * This class serves as the base for implementation factory
 * classes used in the SE/EV projects
 * It will look up and instantiate a class based on a
 * configuration property
 */
abstract public class V2GImplementationFactory {

	/**
	 * Builds an object instance from the configuration properties
	 * The configuration should hold the class of the instance that
	 * will be built.
	 * @param propertyName Name of the property that contains the fully qualified class name
	 * @param cls Target class of the build instance
	 * @param params Optional arguments to the constructor
	 * @return
	 */
	protected static <T> T buildFromProperties(String propertyName, Class<T> cls, Object...params) {
		try {
			String className = MiscUtils.getV2gEntityConfig().getProperty(propertyName);
			if (className == null) {
				return null;
			}
			
			Class<?> clazz = Class.forName(className);
			
			Class<?>[] paramClasses = Arrays.stream(params)
					.map(param -> param.getClass())
					.toArray(size -> new Class<?>[size]);
			
			Constructor<?> constructor = clazz.getConstructor(paramClasses);
			Object instance = constructor.newInstance(params);
			if (!cls.isInstance(instance)) {
				throw new Exception("Instantiated object does not match the expected type " + cls.getCanonicalName());
			}
			return cls.cast(instance);
		} catch (Exception e) {
			throw new RuntimeException("Could not instantiate implementations class for property " + propertyName, e);
		}
	}

	
}
