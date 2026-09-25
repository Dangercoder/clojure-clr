/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 *   the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

using System;
using System.Runtime.CompilerServices;

namespace clojure.lang
{
    /// <summary>
    /// Array copies for the persistent collections' internal arrays. Array.Clone goes through
    /// the runtime's native clone path (slow-path allocation, a transition into native code);
    /// allocating with new T[] and copying stays on the JIT's fast allocation path. Only for
    /// arrays created as exactly T[] (the result is always a T[], never a covariant subtype).
    /// </summary>
    internal static class ArrayUtil
    {
        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public static T[] CopyOf<T>(T[] src)
        {
            // One bulk copy with a single card-marking pass: faster than both Array.Clone and
            // an element loop (a write-barrier call per reference on ARM64) at every size.
            var dst = new T[src.Length];
            src.AsSpan().CopyTo(dst);
            return dst;
        }
    }
}
