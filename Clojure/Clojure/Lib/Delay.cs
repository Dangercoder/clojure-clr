/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/**
 *   Author: David Miller
 **/

using System;
using System.Runtime.CompilerServices;
using System.Threading;

// Realizing a LazySeq/Delay takes a reentrant lock. System.Threading.Lock (.NET 9+) is the
// runtime's lightweight lock; older frameworks use Monitor on a plain object. Both replace
// a ReaderWriterLockSlim per instance, of which only the write side was ever used.
#if NET9_0_OR_GREATER
using RealizeLock = System.Threading.Lock;
#else
using RealizeLock = System.Object;
#endif

namespace clojure.lang
{
    /// <summary>
    /// Implements a delay of a function call.
    /// </summary>
    public class Delay : IDeref, IPending
    {
        #region Data

        /// <summary>
        /// The value, after it has been computed.
        /// </summary>
        object _val;

        /// <summary>
        /// Cached exception, if encountered
        /// </summary>
        Exception _exception;

        /// <summary>
        /// The function being delayed.
        /// </summary>
        IFn _fn;

        RealizeLock _lock = new RealizeLock();

        #endregion

        #region C-tors

        /// <summary>
        /// Construct a delay for a function.
        /// </summary>
        /// <param name="fn">The function to delay.</param>
        public Delay(IFn fn)
        {
            _fn = fn;
            _val = null;
            _exception = null;
        }

        #endregion

        #region Delay operations

        /// <summary>
        /// Force a delay (or identity if not a delay).
        /// </summary>
        /// <param name="x">The object to force.</param>
        /// <returns>The computed valued (if a delay); the object itself (if not a delay).</returns>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Style", "IDE1006:Naming Styles", Justification = "ClojureJVM name match")]
        public static object force(object x)
        {
            return x is Delay delay
                ? delay.deref()
                : x;
        }

        #endregion

        #region IDeref Members

        private void Realize()
        {
            var lk = _lock;
            if ( lk != null )
            {
                lock (lk)
                {
                    if (_fn != null)
                    {
                        try
                        {
                            _val = _fn.invoke();
                        }
                        catch (Exception e)
                        {
                            _exception = e;
                        }
                        _fn = null;
                        _lock = null;
                    }
                }
            }
        }

        /// <summary>
        /// Get the value.
        /// </summary>
        /// <returns>The value</returns>
        /// <remarks>Forces the computation if it has not happened yet.</remarks>
        public object deref()
        {
            if (_lock != null)
                Realize();

            if (_exception != null)
                throw _exception;
         
            return _val;
        }


        #endregion

        #region IPending members

        public bool isRealized()
        {
            return _lock == null;
        }

        #endregion
    }
}
