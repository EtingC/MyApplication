
/** vim: et:ts=4:sw=4:sts=4
 * @license RequireJS 2.1.17 Copyright (c) 2010-2015, The Dojo Foundation All Rights Reserved.
 * Available via the MIT or new BSD license.
 * see: http://github.com/jrburke/requirejs for details
 */
//Not using strict: uneven strict support in browsers, #392, and causes
//problems with requirejs.exec()/transpiler plugins that may not be strict.
/*jslint regexp: true, nomen: true, sloppy: true */
/*global window, navigator, document, importScripts, setTimeout, opera */

var requirejs, require, define, DA = {};
(function (global) {
    var req, s, head, baseElement, dataMain, src,
        interactiveScript, currentlyAddingScript, mainScript, subPath,
        version = '2.1.17',
        commentRegExp = /(\/\*([\s\S]*?)\*\/|([^:]|^)\/\/(.*)$)/mg,
        cjsRequireRegExp = /[^.]\s*require\s*\(\s*["']([^'"\s]+)["']\s*\)/g,
        jsSuffixRegExp = /\.js$/,
        currDirRegExp = /^\.\//,
        op = Object.prototype,
        ostring = op.toString,
        hasOwn = op.hasOwnProperty,
        ap = Array.prototype,
        apsp = ap.splice,
        isBrowser = !!(typeof window !== 'undefined' && typeof navigator !== 'undefined' && window.document),
        isWebWorker = !isBrowser && typeof importScripts !== 'undefined',
        //PS3 indicates loaded and complete, but need to wait for complete
        //specifically. Sequence is 'loading', 'loaded', execution,
        // then 'complete'. The UA check is unfortunate, but not sure how
        //to feature test w/o causing perf issues.
        readyRegExp = isBrowser && navigator.platform === 'PLAYSTATION 3' ?
                      /^complete$/ : /^(complete|loaded)$/,
        defContextName = '_',
        //Oh the tragedy, detecting opera. See the usage of isOpera for reason.
        isOpera = typeof opera !== 'undefined' && opera.toString() === '[object Opera]',
        contexts = {},
        cfg = {},
        globalDefQueue = [],
        useInteractive = false;

    function isFunction(it) {
        return ostring.call(it) === '[object Function]';
    }

    function isArray(it) {
        return ostring.call(it) === '[object Array]';
    }

    /**
     * Helper function for iterating over an array. If the func returns
     * a true value, it will break out of the loop.
     */
    function each(ary, func) {
        if (ary) {
            var i;
            for (i = 0; i < ary.length; i += 1) {
                if (ary[i] && func(ary[i], i, ary)) {
                    break;
                }
            }
        }
    }

    /**
     * Helper function for iterating over an array backwards. If the func
     * returns a true value, it will break out of the loop.
     */
    function eachReverse(ary, func) {
        if (ary) {
            var i;
            for (i = ary.length - 1; i > -1; i -= 1) {
                if (ary[i] && func(ary[i], i, ary)) {
                    break;
                }
            }
        }
    }

    function hasProp(obj, prop) {
        return hasOwn.call(obj, prop);
    }

    function getOwn(obj, prop) {
        return hasProp(obj, prop) && obj[prop];
    }

    /**
     * Cycles over properties in an object and calls a function for each
     * property value. If the function returns a truthy value, then the
     * iteration is stopped.
     */
    function eachProp(obj, func) {
        var prop;
        for (prop in obj) {
            if (hasProp(obj, prop)) {
                if (func(obj[prop], prop)) {
                    break;
                }
            }
        }
    }

    /**
     * Simple function to mix in properties from source into target,
     * but only if target does not already have a property of the same name.
     */
    function mixin(target, source, force, deepStringMixin) {
        if (source) {
            eachProp(source, function (value, prop) {
                if (force || !hasProp(target, prop)) {
                    if (deepStringMixin && typeof value === 'object' && value &&
                        !isArray(value) && !isFunction(value) &&
                        !(value instanceof RegExp)) {

                        if (!target[prop]) {
                            target[prop] = {};
                        }
                        mixin(target[prop], value, force, deepStringMixin);
                    } else {
                        target[prop] = value;
                    }
                }
            });
        }
        return target;
    }

    //Similar to Function.prototype.bind, but the 'this' object is specified
    //first, since it is easier to read/figure out what 'this' will be.
    function bind(obj, fn) {
        return function () {
            return fn.apply(obj, arguments);
        };
    }

    function scripts() {
        return document.getElementsByTagName('script');
    }

    function defaultOnError(err) {
        throw err;
    }

    //Allow getting a global that is expressed in
    //dot notation, like 'a.b.c'.
    function getGlobal(value) {
        if (!value) {
            return value;
        }
        var g = global;
        each(value.split('.'), function (part) {
            g = g[part];
        });
        return g;
    }

    /**
     * Constructs an error with a pointer to an URL with more information.
     * @param {String} id the error ID that maps to an ID on a web page.
     * @param {String} message human readable error.
     * @param {Error} [err] the original error, if there is one.
     *
     * @returns {Error}
     */
    function makeError(id, msg, err, requireModules) {
        var e = new Error(msg + '\nhttp://requirejs.org/docs/errors.html#' + id);
        e.requireType = id;
        e.requireModules = requireModules;
        if (err) {
            e.originalError = err;
        }
        return e;
    }

    if (typeof define !== 'undefined') {
        //If a define is already in play via another AMD loader,
        //do not overwrite.
        return;
    }

    if (typeof requirejs !== 'undefined') {
        if (isFunction(requirejs)) {
            //Do not overwrite an existing requirejs instance.
            return;
        }
        cfg = requirejs;
        requirejs = undefined;
    }

    //Allow for a require config object
    if (typeof require !== 'undefined' && !isFunction(require)) {
        //assume it is a config object.
        cfg = require;
        require = undefined;
    }

    function newContext(contextName) {
        var inCheckLoaded, Module, context, handlers,
            checkLoadedTimeoutId,
            config = {
                //Defaults. Do not set a default for map
                //config to speed up normalize(), which
                //will run faster if there is no default.
                waitSeconds: 7,
                baseUrl: './',
                paths: {},
                bundles: {},
                pkgs: {},
                shim: {},
                config: {}
            },
            registry = {},
            //registry of just enabled modules, to speed
            //cycle breaking code when lots of modules
            //are registered, but not activated.
            enabledRegistry = {},
            undefEvents = {},
            defQueue = [],
            defined = {},
            urlFetched = {},
            bundlesMap = {},
            requireCounter = 1,
            unnormalizedCounter = 1;

        /**
         * Trims the . and .. from an array of path segments.
         * It will keep a leading path segment if a .. will become
         * the first path segment, to help with module name lookups,
         * which act like paths, but can be remapped. But the end result,
         * all paths that use this function should look normalized.
         * NOTE: this method MODIFIES the input array.
         * @param {Array} ary the array of path segments.
         */
        function trimDots(ary) {
            var i, part;
            for (i = 0; i < ary.length; i++) {
                part = ary[i];
                if (part === '.') {
                    ary.splice(i, 1);
                    i -= 1;
                } else if (part === '..') {
                    // If at the start, or previous value is still ..,
                    // keep them so that when converted to a path it may
                    // still work when converted to a path, even though
                    // as an ID it is less than ideal. In larger point
                    // releases, may be better to just kick out an error.
                    if (i === 0 || (i === 1 && ary[2] === '..') || ary[i - 1] === '..') {
                        continue;
                    } else if (i > 0) {
                        ary.splice(i - 1, 2);
                        i -= 2;
                    }
                }
            }
        }

        /**
         * Given a relative module name, like ./something, normalize it to
         * a real name that can be mapped to a path.
         * @param {String} name the relative name
         * @param {String} baseName a real name that the name arg is relative
         * to.
         * @param {Boolean} applyMap apply the map config to the value. Should
         * only be done if this normalization is for a dependency ID.
         * @returns {String} normalized name
         */
        function normalize(name, baseName, applyMap) {
            var pkgMain, mapValue, nameParts, i, j, nameSegment, lastIndex,
                foundMap, foundI, foundStarMap, starI, normalizedBaseParts,
                baseParts = (baseName && baseName.split('/')),
                map = config.map,
                starMap = map && map['*'];

            //Adjust any relative paths.
            if (name) {
                name = name.split('/');
                lastIndex = name.length - 1;

                // If wanting node ID compatibility, strip .js from end
                // of IDs. Have to do this here, and not in nameToUrl
                // because node allows either .js or non .js to map
                // to same file.
                if (config.nodeIdCompat && jsSuffixRegExp.test(name[lastIndex])) {
                    name[lastIndex] = name[lastIndex].replace(jsSuffixRegExp, '');
                }

                // Starts with a '.' so need the baseName
                if (name[0].charAt(0) === '.' && baseParts) {
                    //Convert baseName to array, and lop off the last part,
                    //so that . matches that 'directory' and not name of the baseName's
                    //module. For instance, baseName of 'one/two/three', maps to
                    //'one/two/three.js', but we want the directory, 'one/two' for
                    //this normalization.
                    normalizedBaseParts = baseParts.slice(0, baseParts.length - 1);
                    name = normalizedBaseParts.concat(name);
                }

                trimDots(name);
                name = name.join('/');
            }

            //Apply map config if available.
            if (applyMap && map && (baseParts || starMap)) {
                nameParts = name.split('/');

                outerLoop: for (i = nameParts.length; i > 0; i -= 1) {
                    nameSegment = nameParts.slice(0, i).join('/');

                    if (baseParts) {
                        //Find the longest baseName segment match in the config.
                        //So, do joins on the biggest to smallest lengths of baseParts.
                        for (j = baseParts.length; j > 0; j -= 1) {
                            mapValue = getOwn(map, baseParts.slice(0, j).join('/'));

                            //baseName segment has config, find if it has one for
                            //this name.
                            if (mapValue) {
                                mapValue = getOwn(mapValue, nameSegment);
                                if (mapValue) {
                                    //Match, update name to the new value.
                                    foundMap = mapValue;
                                    foundI = i;
                                    break outerLoop;
                                }
                            }
                        }
                    }

                    //Check for a star map match, but just hold on to it,
                    //if there is a shorter segment match later in a matching
                    //config, then favor over this star map.
                    if (!foundStarMap && starMap && getOwn(starMap, nameSegment)) {
                        foundStarMap = getOwn(starMap, nameSegment);
                        starI = i;
                    }
                }

                if (!foundMap && foundStarMap) {
                    foundMap = foundStarMap;
                    foundI = starI;
                }

                if (foundMap) {
                    nameParts.splice(0, foundI, foundMap);
                    name = nameParts.join('/');
                }
            }

            // If the name points to a package's name, use
            // the package main instead.
            pkgMain = getOwn(config.pkgs, name);

            return pkgMain ? pkgMain : name;
        }

        function removeScript(name) {
            if (isBrowser) {
                each(scripts(), function (scriptNode) {
                    if (scriptNode.getAttribute('data-requiremodule') === name &&
                            scriptNode.getAttribute('data-requirecontext') === context.contextName) {
                        scriptNode.parentNode.removeChild(scriptNode);
                        return true;
                    }
                });
            }
        }

        function hasPathFallback(id) {
            var pathConfig = getOwn(config.paths, id);
            if (pathConfig && isArray(pathConfig) && pathConfig.length > 1) {
                //Pop off the first array value, since it failed, and
                //retry
                pathConfig.shift();
                context.require.undef(id);

                //Custom require that does not do map translation, since
                //ID is "absolute", already mapped/resolved.
                context.makeRequire(null, {
                    skipMap: true
                })([id]);

                return true;
            }
        }

        //Turns a plugin!resource to [plugin, resource]
        //with the plugin being undefined if the name
        //did not have a plugin prefix.
        function splitPrefix(name) {
            var prefix,
                index = name ? name.indexOf('!') : -1;
            if (index > -1) {
                prefix = name.substring(0, index);
                name = name.substring(index + 1, name.length);
            }
            return [prefix, name];
        }

        /**
         * Creates a module mapping that includes plugin prefix, module
         * name, and path. If parentModuleMap is provided it will
         * also normalize the name via require.normalize()
         *
         * @param {String} name the module name
         * @param {String} [parentModuleMap] parent module map
         * for the module name, used to resolve relative names.
         * @param {Boolean} isNormalized: is the ID already normalized.
         * This is true if this call is done for a define() module ID.
         * @param {Boolean} applyMap: apply the map config to the ID.
         * Should only be true if this map is for a dependency.
         *
         * @returns {Object}
         */
        function makeModuleMap(name, parentModuleMap, isNormalized, applyMap) {
            var url, pluginModule, suffix, nameParts,
                prefix = null,
                parentName = parentModuleMap ? parentModuleMap.name : null,
                originalName = name,
                isDefine = true,
                normalizedName = '';

            //If no name, then it means it is a require call, generate an
            //internal name.
            if (!name) {
                isDefine = false;
                name = '_@r' + (requireCounter += 1);
            }

            nameParts = splitPrefix(name);
            prefix = nameParts[0];
            name = nameParts[1];

            if (prefix) {
                prefix = normalize(prefix, parentName, applyMap);
                pluginModule = getOwn(defined, prefix);
            }

            //Account for relative paths if there is a base name.
            if (name) {
                if (prefix) {
                    if (pluginModule && pluginModule.normalize) {
                        //Plugin is loaded, use its normalize method.
                        normalizedName = pluginModule.normalize(name, function (name) {
                            return normalize(name, parentName, applyMap);
                        });
                    } else {
                        // If nested plugin references, then do not try to
                        // normalize, as it will not normalize correctly. This
                        // places a restriction on resourceIds, and the longer
                        // term solution is not to normalize until plugins are
                        // loaded and all normalizations to allow for async
                        // loading of a loader plugin. But for now, fixes the
                        // common uses. Details in #1131
                        normalizedName = name.indexOf('!') === -1 ?
                                         normalize(name, parentName, applyMap) :
                                         name;
                    }
                } else {
                    //A regular module.
                    normalizedName = normalize(name, parentName, applyMap);

                    //Normalized name may be a plugin ID due to map config
                    //application in normalize. The map config values must
                    //already be normalized, so do not need to redo that part.
                    nameParts = splitPrefix(normalizedName);
                    prefix = nameParts[0];
                    normalizedName = nameParts[1];
                    isNormalized = true;

                    url = context.nameToUrl(normalizedName);
                }
            }

            //If the id is a plugin id that cannot be determined if it needs
            //normalization, stamp it with a unique ID so two matching relative
            //ids that may conflict can be separate.
            suffix = prefix && !pluginModule && !isNormalized ?
                     '_unnormalized' + (unnormalizedCounter += 1) :
                     '';

            return {
                prefix: prefix,
                name: normalizedName,
                parentMap: parentModuleMap,
                unnormalized: !!suffix,
                url: url,
                originalName: originalName,
                isDefine: isDefine,
                id: (prefix ?
                        prefix + '!' + normalizedName :
                        normalizedName) + suffix
            };
        }

        function getModule(depMap) {
            var id = depMap.id,
                mod = getOwn(registry, id);

            if (!mod) {
                mod = registry[id] = new context.Module(depMap);
            }

            return mod;
        }

        function on(depMap, name, fn) {
            var id = depMap.id,
                mod = getOwn(registry, id);

            if (hasProp(defined, id) &&
                    (!mod || mod.defineEmitComplete)) {
                if (name === 'defined') {
                    fn(defined[id]);
                }
            } else {
                mod = getModule(depMap);
                if (mod.error && name === 'error') {
                    fn(mod.error);
                } else {
                    mod.on(name, fn);
                }
            }
        }

        function onError(err, errback) {
            var ids = err.requireModules,
                notified = false;

            if (errback) {
                errback(err);
            } else {
                each(ids, function (id) {
                    var mod = getOwn(registry, id);
                    if (mod) {
                        //Set error on module, so it skips timeout checks.
                        mod.error = err;
                        if (mod.events.error) {
                            notified = true;
                            mod.emit('error', err);
                        }
                    }
                });

                if (!notified) {
                    req.onError(err);
                }
            }
        }

        /**
         * Internal method to transfer globalQueue items to this context's
         * defQueue.
         */
        function takeGlobalQueue() {
            //Push all the globalDefQueue items into the context's defQueue
            if (globalDefQueue.length) {
                //Array splice in the values since the context code has a
                //local var ref to defQueue, so cannot just reassign the one
                //on context.
                apsp.apply(defQueue,
                           [defQueue.length, 0].concat(globalDefQueue));
                globalDefQueue = [];
            }
        }

        handlers = {
            'require': function (mod) {
                if (mod.require) {
                    return mod.require;
                } else {
                    return (mod.require = context.makeRequire(mod.map));
                }
            },
            'exports': function (mod) {
                mod.usingExports = true;
                if (mod.map.isDefine) {
                    if (mod.exports) {
                        return (defined[mod.map.id] = mod.exports);
                    } else {
                        return (mod.exports = defined[mod.map.id] = {});
                    }
                }
            },
            'module': function (mod) {
                if (mod.module) {
                    return mod.module;
                } else {
                    return (mod.module = {
                        id: mod.map.id,
                        uri: mod.map.url,
                        config: function () {
                            return  getOwn(config.config, mod.map.id) || {};
                        },
                        exports: mod.exports || (mod.exports = {})
                    });
                }
            }
        };

        function cleanRegistry(id) {
            //Clean up machinery used for waiting modules.
            delete registry[id];
            delete enabledRegistry[id];
        }

        function breakCycle(mod, traced, processed) {
            var id = mod.map.id;

            if (mod.error) {
                mod.emit('error', mod.error);
            } else {
                traced[id] = true;
                each(mod.depMaps, function (depMap, i) {
                    var depId = depMap.id,
                        dep = getOwn(registry, depId);

                    //Only force things that have not completed
                    //being defined, so still in the registry,
                    //and only if it has not been matched up
                    //in the module already.
                    if (dep && !mod.depMatched[i] && !processed[depId]) {
                        if (getOwn(traced, depId)) {
                            mod.defineDep(i, defined[depId]);
                            mod.check(); //pass false?
                        } else {
                            breakCycle(dep, traced, processed);
                        }
                    }
                });
                processed[id] = true;
            }
        }

        function checkLoaded() {
            var err, usingPathFallback,
                waitInterval = config.waitSeconds * 1000,
                //It is possible to disable the wait interval by using waitSeconds of 0.
                expired = waitInterval && (context.startTime + waitInterval) < new Date().getTime(),
                noLoads = [],
                reqCalls = [],
                stillLoading = false,
                needCycleCheck = true;

            //Do not bother if this call was a result of a cycle break.
            if (inCheckLoaded) {
                return;
            }

            inCheckLoaded = true;

            //Figure out the state of all the modules.
            eachProp(enabledRegistry, function (mod) {
                var map = mod.map,
                    modId = map.id;

                //Skip things that are not enabled or in error state.
                if (!mod.enabled) {
                    return;
                }

                if (!map.isDefine) {
                    reqCalls.push(mod);
                }

                if (!mod.error) {
                    //If the module should be executed, and it has not
                    //been inited and time is up, remember it.
                    if (!mod.inited && expired) {
                        if (hasPathFallback(modId)) {
                            usingPathFallback = true;
                            stillLoading = true;
                        } else {
                            noLoads.push(modId);
                            removeScript(modId);
                        }
                    } else if (!mod.inited && mod.fetched && map.isDefine) {
                        stillLoading = true;
                        if (!map.prefix) {
                            //No reason to keep looking for unfinished
                            //loading. If the only stillLoading is a
                            //plugin resource though, keep going,
                            //because it may be that a plugin resource
                            //is waiting on a non-plugin cycle.
                            return (needCycleCheck = false);
                        }
                    }
                }
            });

            if (expired && noLoads.length) {
                //If wait time expired, throw error of unloaded modules.
                err = makeError('timeout', 'Load timeout for modules: ' + noLoads, null, noLoads);
                err.contextName = context.contextName;
                return onError(err);
            }

            //Not expired, check for a cycle.
            if (needCycleCheck) {
                each(reqCalls, function (mod) {
                    breakCycle(mod, {}, {});
                });
            }

            //If still waiting on loads, and the waiting load is something
            //other than a plugin resource, or there are still outstanding
            //scripts, then just try back later.
            if ((!expired || usingPathFallback) && stillLoading) {
                //Something is still waiting to load. Wait for it, but only
                //if a timeout is not already in effect.
                if ((isBrowser || isWebWorker) && !checkLoadedTimeoutId) {
                    checkLoadedTimeoutId = setTimeout(function () {
                        checkLoadedTimeoutId = 0;
                        checkLoaded();
                    }, 50);
                }
            }

            inCheckLoaded = false;
        }

        Module = function (map) {
            this.events = getOwn(undefEvents, map.id) || {};
            this.map = map;
            this.shim = getOwn(config.shim, map.id);
            this.depExports = [];
            this.depMaps = [];
            this.depMatched = [];
            this.pluginMaps = {};
            this.depCount = 0;

            /* this.exports this.factory
               this.depMaps = [],
               this.enabled, this.fetched
            */
        };

        Module.prototype = {
            init: function (depMaps, factory, errback, options) {
                options = options || {};

                //Do not do more inits if already done. Can happen if there
                //are multiple define calls for the same module. That is not
                //a normal, common case, but it is also not unexpected.
                if (this.inited) {
                    return;
                }

                this.factory = factory;

                if (errback) {
                    //Register for errors on this module.
                    this.on('error', errback);
                } else if (this.events.error) {
                    //If no errback already, but there are error listeners
                    //on this module, set up an errback to pass to the deps.
                    errback = bind(this, function (err) {
                        this.emit('error', err);
                    });
                }

                //Do a copy of the dependency array, so that
                //source inputs are not modified. For example
                //"shim" deps are passed in here directly, and
                //doing a direct modification of the depMaps array
                //would affect that config.
                this.depMaps = depMaps && depMaps.slice(0);

                this.errback = errback;

                //Indicate this module has be initialized
                this.inited = true;

                this.ignore = options.ignore;

                //Could have option to init this module in enabled mode,
                //or could have been previously marked as enabled. However,
                //the dependencies are not known until init is called. So
                //if enabled previously, now trigger dependencies as enabled.
                if (options.enabled || this.enabled) {
                    //Enable this module and dependencies.
                    //Will call this.check()
                    this.enable();
                } else {
                    this.check();
                }
            },

            defineDep: function (i, depExports) {
                //Because of cycles, defined callback for a given
                //export can be called more than once.
                if (!this.depMatched[i]) {
                    this.depMatched[i] = true;
                    this.depCount -= 1;
                    this.depExports[i] = depExports;
                }
            },

            fetch: function () {
                if (this.fetched) {
                    return;
                }
                this.fetched = true;

                context.startTime = (new Date()).getTime();

                var map = this.map;

                //If the manager is for a plugin managed resource,
                //ask the plugin to load it now.
                if (this.shim) {
                    context.makeRequire(this.map, {
                        enableBuildCallback: true
                    })(this.shim.deps || [], bind(this, function () {
                        return map.prefix ? this.callPlugin() : this.load();
                    }));
                } else {
                    //Regular dependency.
                    return map.prefix ? this.callPlugin() : this.load();
                }
            },

            load: function () {
                var url = this.map.url;

                //Regular dependency.
                if (!urlFetched[url]) {
                    urlFetched[url] = true;
                    context.load(this.map.id, url);
                }
            },

            /**
             * Checks if the module is ready to define itself, and if so,
             * define it.
             */
            check: function () {
                if (!this.enabled || this.enabling) {
                    return;
                }

                var err, cjsModule,
                    id = this.map.id,
                    depExports = this.depExports,
                    exports = this.exports,
                    factory = this.factory;

                if (!this.inited) {
                    this.fetch();
                } else if (this.error) {
                    this.emit('error', this.error);
                } else if (!this.defining) {
                    //The factory could trigger another require call
                    //that would result in checking this module to
                    //define itself again. If already in the process
                    //of doing that, skip this work.
                    this.defining = true;

                    if (this.depCount < 1 && !this.defined) {
                        if (isFunction(factory)) {
                            //If there is an error listener, favor passing
                            //to that instead of throwing an error. However,
                            //only do it for define()'d  modules. require
                            //errbacks should not be called for failures in
                            //their callbacks (#699). However if a global
                            //onError is set, use that.
                            if ((this.events.error && this.map.isDefine) ||
                                req.onError !== defaultOnError) {
                                try {
                                    exports = context.execCb(id, factory, depExports, exports);
                                } catch (e) {
                                    err = e;
                                }
                            } else {
                                exports = context.execCb(id, factory, depExports, exports);
                            }

                            // Favor return value over exports. If node/cjs in play,
                            // then will not have a return value anyway. Favor
                            // module.exports assignment over exports object.
                            if (this.map.isDefine && exports === undefined) {
                                cjsModule = this.module;
                                if (cjsModule) {
                                    exports = cjsModule.exports;
                                } else if (this.usingExports) {
                                    //exports already set the defined value.
                                    exports = this.exports;
                                }
                            }

                            if (err) {
                                err.requireMap = this.map;
                                err.requireModules = this.map.isDefine ? [this.map.id] : null;
                                err.requireType = this.map.isDefine ? 'define' : 'require';
                                return onError((this.error = err));
                            }

                        } else {
                            //Just a literal value
                            exports = factory;
                        }

                        this.exports = exports;

                        if (this.map.isDefine && !this.ignore) {
                            defined[id] = exports;

                            if (req.onResourceLoad) {
                                req.onResourceLoad(context, this.map, this.depMaps);
                            }
                        }

                        //Clean up
                        cleanRegistry(id);

                        this.defined = true;
                    }

                    //Finished the define stage. Allow calling check again
                    //to allow define notifications below in the case of a
                    //cycle.
                    this.defining = false;

                    if (this.defined && !this.defineEmitted) {
                        this.defineEmitted = true;
                        this.emit('defined', this.exports);
                        this.defineEmitComplete = true;
                    }

                }
            },

            callPlugin: function () {
                var map = this.map,
                    id = map.id,
                    //Map already normalized the prefix.
                    pluginMap = makeModuleMap(map.prefix);

                //Mark this as a dependency for this plugin, so it
                //can be traced for cycles.
                this.depMaps.push(pluginMap);

                on(pluginMap, 'defined', bind(this, function (plugin) {
                    var load, normalizedMap, normalizedMod,
                        bundleId = getOwn(bundlesMap, this.map.id),
                        name = this.map.name,
                        parentName = this.map.parentMap ? this.map.parentMap.name : null,
                        localRequire = context.makeRequire(map.parentMap, {
                            enableBuildCallback: true
                        });

                    //If current map is not normalized, wait for that
                    //normalized name to load instead of continuing.
                    if (this.map.unnormalized) {
                        //Normalize the ID if the plugin allows it.
                        if (plugin.normalize) {
                            name = plugin.normalize(name, function (name) {
                                return normalize(name, parentName, true);
                            }) || '';
                        }

                        //prefix and name should already be normalized, no need
                        //for applying map config again either.
                        normalizedMap = makeModuleMap(map.prefix + '!' + name,
                                                      this.map.parentMap);
                        on(normalizedMap,
                            'defined', bind(this, function (value) {
                                this.init([], function () { return value; }, null, {
                                    enabled: true,
                                    ignore: true
                                });
                            }));

                        normalizedMod = getOwn(registry, normalizedMap.id);
                        if (normalizedMod) {
                            //Mark this as a dependency for this plugin, so it
                            //can be traced for cycles.
                            this.depMaps.push(normalizedMap);

                            if (this.events.error) {
                                normalizedMod.on('error', bind(this, function (err) {
                                    this.emit('error', err);
                                }));
                            }
                            normalizedMod.enable();
                        }

                        return;
                    }

                    //If a paths config, then just load that file instead to
                    //resolve the plugin, as it is built into that paths layer.
                    if (bundleId) {
                        this.map.url = context.nameToUrl(bundleId);
                        this.load();
                        return;
                    }

                    load = bind(this, function (value) {
                        this.init([], function () { return value; }, null, {
                            enabled: true
                        });
                    });

                    load.error = bind(this, function (err) {
                        this.inited = true;
                        this.error = err;
                        err.requireModules = [id];

                        //Remove temp unnormalized modules for this module,
                        //since they will never be resolved otherwise now.
                        eachProp(registry, function (mod) {
                            if (mod.map.id.indexOf(id + '_unnormalized') === 0) {
                                cleanRegistry(mod.map.id);
                            }
                        });

                        onError(err);
                    });

                    //Allow plugins to load other code without having to know the
                    //context or how to 'complete' the load.
                    load.fromText = bind(this, function (text, textAlt) {
                        /*jslint evil: true */
                        var moduleName = map.name,
                            moduleMap = makeModuleMap(moduleName),
                            hasInteractive = useInteractive;

                        //As of 2.1.0, support just passing the text, to reinforce
                        //fromText only being called once per resource. Still
                        //support old style of passing moduleName but discard
                        //that moduleName in favor of the internal ref.
                        if (textAlt) {
                            text = textAlt;
                        }

                        //Turn off interactive script matching for IE for any define
                        //calls in the text, then turn it back on at the end.
                        if (hasInteractive) {
                            useInteractive = false;
                        }

                        //Prime the system by creating a module instance for
                        //it.
                        getModule(moduleMap);

                        //Transfer any config to this other module.
                        if (hasProp(config.config, id)) {
                            config.config[moduleName] = config.config[id];
                        }

                        try {
                            req.exec(text);
                        } catch (e) {
                            return onError(makeError('fromtexteval',
                                             'fromText eval for ' + id +
                                            ' failed: ' + e,
                                             e,
                                             [id]));
                        }

                        if (hasInteractive) {
                            useInteractive = true;
                        }

                        //Mark this as a dependency for the plugin
                        //resource
                        this.depMaps.push(moduleMap);

                        //Support anonymous modules.
                        context.completeLoad(moduleName);

                        //Bind the value of that module to the value for this
                        //resource ID.
                        localRequire([moduleName], load);
                    });

                    //Use parentName here since the plugin's name is not reliable,
                    //could be some weird string with no path that actually wants to
                    //reference the parentName's path.
                    plugin.load(map.name, localRequire, load, config);
                }));

                context.enable(pluginMap, this);
                this.pluginMaps[pluginMap.id] = pluginMap;
            },

            enable: function () {
                enabledRegistry[this.map.id] = this;
                this.enabled = true;

                //Set flag mentioning that the module is enabling,
                //so that immediate calls to the defined callbacks
                //for dependencies do not trigger inadvertent load
                //with the depCount still being zero.
                this.enabling = true;

                //Enable each dependency
                each(this.depMaps, bind(this, function (depMap, i) {
                    var id, mod, handler;

                    if (typeof depMap === 'string') {
                        //Dependency needs to be converted to a depMap
                        //and wired up to this module.
                        depMap = makeModuleMap(depMap,
                                               (this.map.isDefine ? this.map : this.map.parentMap),
                                               false,
                                               !this.skipMap);
                        this.depMaps[i] = depMap;

                        handler = getOwn(handlers, depMap.id);

                        if (handler) {
                            this.depExports[i] = handler(this);
                            return;
                        }

                        this.depCount += 1;

                        on(depMap, 'defined', bind(this, function (depExports) {
                            this.defineDep(i, depExports);
                            this.check();
                        }));

                        if (this.errback) {
                            on(depMap, 'error', bind(this, this.errback));
                        } else if (this.events.error) {
                            // No direct errback on this module, but something
                            // else is listening for errors, so be sure to
                            // propagate the error correctly.
                            on(depMap, 'error', bind(this, function(err) {
                                this.emit('error', err);
                            }));
                        }
                    }

                    id = depMap.id;
                    mod = registry[id];

                    //Skip special modules like 'require', 'exports', 'module'
                    //Also, don't call enable if it is already enabled,
                    //important in circular dependency cases.
                    if (!hasProp(handlers, id) && mod && !mod.enabled) {
                        context.enable(depMap, this);
                    }
                }));

                //Enable each plugin that is used in
                //a dependency
                eachProp(this.pluginMaps, bind(this, function (pluginMap) {
                    var mod = getOwn(registry, pluginMap.id);
                    if (mod && !mod.enabled) {
                        context.enable(pluginMap, this);
                    }
                }));

                this.enabling = false;

                this.check();
            },

            on: function (name, cb) {
                var cbs = this.events[name];
                if (!cbs) {
                    cbs = this.events[name] = [];
                }
                cbs.push(cb);
            },

            emit: function (name, evt) {
                each(this.events[name], function (cb) {
                    cb(evt);
                });
                if (name === 'error') {
                    //Now that the error handler was triggered, remove
                    //the listeners, since this broken Module instance
                    //can stay around for a while in the registry.
                    delete this.events[name];
                }
            }
        };

        function callGetModule(args) {
            //Skip modules already defined.
            if (!hasProp(defined, args[0])) {
                getModule(makeModuleMap(args[0], null, true)).init(args[1], args[2]);
            }
        }

        function removeListener(node, func, name, ieName) {
            //Favor detachEvent because of IE9
            //issue, see attachEvent/addEventListener comment elsewhere
            //in this file.
            if (node.detachEvent && !isOpera) {
                //Probably IE. If not it will throw an error, which will be
                //useful to know.
                if (ieName) {
                    node.detachEvent(ieName, func);
                }
            } else {
                node.removeEventListener(name, func, false);
            }
        }

        /**
         * Given an event from a script node, get the requirejs info from it,
         * and then removes the event listeners on the node.
         * @param {Event} evt
         * @returns {Object}
         */
        function getScriptData(evt) {
            //Using currentTarget instead of target for Firefox 2.0's sake. Not
            //all old browsers will be supported, but this one was easy enough
            //to support and still makes sense.
            var node = evt.currentTarget || evt.srcElement;

            //Remove the listeners once here.
            removeListener(node, context.onScriptLoad, 'load', 'onreadystatechange');
            removeListener(node, context.onScriptError, 'error');

            return {
                node: node,
                id: node && node.getAttribute('data-requiremodule')
            };
        }

        function intakeDefines() {
            var args;

            //Any defined modules in the global queue, intake them now.
            takeGlobalQueue();

            //Make sure any remaining defQueue items get properly processed.
            while (defQueue.length) {
                args = defQueue.shift();
                if (args[0] === null) {
                    return onError(makeError('mismatch', 'Mismatched anonymous define() module: ' + args[args.length - 1]));
                } else {
                    //args are id, deps, factory. Should be normalized by the
                    //define() function.
                    callGetModule(args);
                }
            }
        }

        context = {
            config: config,
            contextName: contextName,
            registry: registry,
            defined: defined,
            urlFetched: urlFetched,
            defQueue: defQueue,
            Module: Module,
            makeModuleMap: makeModuleMap,
            nextTick: req.nextTick,
            onError: onError,

            /**
             * Set a configuration for the context.
             * @param {Object} cfg config object to integrate.
             */
            configure: function (cfg) {
                //Make sure the baseUrl ends in a slash.
                if (cfg.baseUrl) {
                    if (cfg.baseUrl.charAt(cfg.baseUrl.length - 1) !== '/') {
                        cfg.baseUrl += '/';
                    }
                }

                //Save off the paths since they require special processing,
                //they are additive.
                var shim = config.shim,
                    objs = {
                        paths: true,
                        bundles: true,
                        config: true,
                        map: true
                    };

                eachProp(cfg, function (value, prop) {
                    if (objs[prop]) {
                        if (!config[prop]) {
                            config[prop] = {};
                        }
                        mixin(config[prop], value, true, true);
                    } else {
                        config[prop] = value;
                    }
                });

                //Reverse map the bundles
                if (cfg.bundles) {
                    eachProp(cfg.bundles, function (value, prop) {
                        each(value, function (v) {
                            if (v !== prop) {
                                bundlesMap[v] = prop;
                            }
                        });
                    });
                }

                //Merge shim
                if (cfg.shim) {
                    eachProp(cfg.shim, function (value, id) {
                        //Normalize the structure
                        if (isArray(value)) {
                            value = {
                                deps: value
                            };
                        }
                        if ((value.exports || value.init) && !value.exportsFn) {
                            value.exportsFn = context.makeShimExports(value);
                        }
                        shim[id] = value;
                    });
                    config.shim = shim;
                }

                //Adjust packages if necessary.
                if (cfg.packages) {
                    each(cfg.packages, function (pkgObj) {
                        var location, name;

                        pkgObj = typeof pkgObj === 'string' ? { name: pkgObj } : pkgObj;

                        name = pkgObj.name;
                        location = pkgObj.location;
                        if (location) {
                            config.paths[name] = pkgObj.location;
                        }

                        //Save pointer to main module ID for pkg name.
                        //Remove leading dot in main, so main paths are normalized,
                        //and remove any trailing .js, since different package
                        //envs have different conventions: some use a module name,
                        //some use a file name.
                        config.pkgs[name] = pkgObj.name + '/' + (pkgObj.main || 'main')
                                     .replace(currDirRegExp, '')
                                     .replace(jsSuffixRegExp, '');
                    });
                }

                //If there are any "waiting to execute" modules in the registry,
                //update the maps for them, since their info, like URLs to load,
                //may have changed.
                eachProp(registry, function (mod, id) {
                    //If module already has init called, since it is too
                    //late to modify them, and ignore unnormalized ones
                    //since they are transient.
                    if (!mod.inited && !mod.map.unnormalized) {
                        mod.map = makeModuleMap(id);
                    }
                });

                //If a deps array or a config callback is specified, then call
                //require with those args. This is useful when require is defined as a
                //config object before require.js is loaded.
                if (cfg.deps || cfg.callback) {
                    context.require(cfg.deps || [], cfg.callback);
                }
            },

            makeShimExports: function (value) {
                function fn() {
                    var ret;
                    if (value.init) {
                        ret = value.init.apply(global, arguments);
                    }
                    return ret || (value.exports && getGlobal(value.exports));
                }
                return fn;
            },

            makeRequire: function (relMap, options) {
                options = options || {};

                function localRequire(deps, callback, errback) {
                    var id, map, requireMod;

                    if (options.enableBuildCallback && callback && isFunction(callback)) {
                        callback.__requireJsBuild = true;
                    }

                    if (typeof deps === 'string') {
                        if (isFunction(callback)) {
                            //Invalid call
                            return onError(makeError('requireargs', 'Invalid require call'), errback);
                        }

                        //If require|exports|module are requested, get the
                        //value for them from the special handlers. Caveat:
                        //this only works while module is being defined.
                        if (relMap && hasProp(handlers, deps)) {
                            return handlers[deps](registry[relMap.id]);
                        }

                        //Synchronous access to one module. If require.get is
                        //available (as in the Node adapter), prefer that.
                        if (req.get) {
                            return req.get(context, deps, relMap, localRequire);
                        }

                        //Normalize module name, if it contains . or ..
                        map = makeModuleMap(deps, relMap, false, true);
                        id = map.id;

                        if (!hasProp(defined, id)) {
                            return onError(makeError('notloaded', 'Module name "' +
                                        id +
                                        '" has not been loaded yet for context: ' +
                                        contextName +
                                        (relMap ? '' : '. Use require([])')));
                        }
                        return defined[id];
                    }

                    //Grab defines waiting in the global queue.
                    intakeDefines();

                    //Mark all the dependencies as needing to be loaded.
                    context.nextTick(function () {
                        //Some defines could have been added since the
                        //require call, collect them.
                        intakeDefines();

                        requireMod = getModule(makeModuleMap(null, relMap));

                        //Store if map config should be applied to this require
                        //call for dependencies.
                        requireMod.skipMap = options.skipMap;

                        requireMod.init(deps, callback, errback, {
                            enabled: true
                        });

                        checkLoaded();
                    });

                    return localRequire;
                }

                mixin(localRequire, {
                    isBrowser: isBrowser,

                    /**
                     * Converts a module name + .extension into an URL path.
                     * *Requires* the use of a module name. It does not support using
                     * plain URLs like nameToUrl.
                     */
                    toUrl: function (moduleNamePlusExt) {
                        var ext,
                            index = moduleNamePlusExt.lastIndexOf('.'),
                            segment = moduleNamePlusExt.split('/')[0],
                            isRelative = segment === '.' || segment === '..';

                        //Have a file extension alias, and it is not the
                        //dots from a relative path.
                        if (index !== -1 && (!isRelative || index > 1)) {
                            ext = moduleNamePlusExt.substring(index, moduleNamePlusExt.length);
                            moduleNamePlusExt = moduleNamePlusExt.substring(0, index);
                        }

                        return context.nameToUrl(normalize(moduleNamePlusExt,
                                                relMap && relMap.id, true), ext,  true);
                    },

                    defined: function (id) {
                        return hasProp(defined, makeModuleMap(id, relMap, false, true).id);
                    },

                    specified: function (id) {
                        id = makeModuleMap(id, relMap, false, true).id;
                        return hasProp(defined, id) || hasProp(registry, id);
                    }
                });

                //Only allow undef on top level require calls
                if (!relMap) {
                    localRequire.undef = function (id) {
                        //Bind any waiting define() calls to this context,
                        //fix for #408
                        takeGlobalQueue();

                        var map = makeModuleMap(id, relMap, true),
                            mod = getOwn(registry, id);

                        removeScript(id);

                        delete defined[id];
                        delete urlFetched[map.url];
                        delete undefEvents[id];

                        //Clean queued defines too. Go backwards
                        //in array so that the splices do not
                        //mess up the iteration.
                        eachReverse(defQueue, function(args, i) {
                            if(args[0] === id) {
                                defQueue.splice(i, 1);
                            }
                        });

                        if (mod) {
                            //Hold on to listeners in case the
                            //module will be attempted to be reloaded
                            //using a different config.
                            if (mod.events.defined) {
                                undefEvents[id] = mod.events;
                            }

                            cleanRegistry(id);
                        }
                    };
                }

                return localRequire;
            },

            /**
             * Called to enable a module if it is still in the registry
             * awaiting enablement. A second arg, parent, the parent module,
             * is passed in for context, when this method is overridden by
             * the optimizer. Not shown here to keep code compact.
             */
            enable: function (depMap) {
                var mod = getOwn(registry, depMap.id);
                if (mod) {
                    getModule(depMap).enable();
                }
            },

            /**
             * Internal method used by environment adapters to complete a load event.
             * A load event could be a script load or just a load pass from a synchronous
             * load call.
             * @param {String} moduleName the name of the module to potentially complete.
             */
            completeLoad: function (moduleName) {
                var found, args, mod,
                    shim = getOwn(config.shim, moduleName) || {},
                    shExports = shim.exports;

                takeGlobalQueue();

                while (defQueue.length) {
                    args = defQueue.shift();
                    if (args[0] === null) {
                        args[0] = moduleName;
                        //If already found an anonymous module and bound it
                        //to this name, then this is some other anon module
                        //waiting for its completeLoad to fire.
                        if (found) {
                            break;
                        }
                        found = true;
                    } else if (args[0] === moduleName) {
                        //Found matching define call for this script!
                        found = true;
                    }

                    callGetModule(args);
                }

                //Do this after the cycle of callGetModule in case the result
                //of those calls/init calls changes the registry.
                mod = getOwn(registry, moduleName);

                if (!found && !hasProp(defined, moduleName) && mod && !mod.inited) {
                    if (config.enforceDefine && (!shExports || !getGlobal(shExports))) {
                        if (hasPathFallback(moduleName)) {
                            return;
                        } else {
                            return onError(makeError('nodefine',
                                             'No define call for ' + moduleName,
                                             null,
                                             [moduleName]));
                        }
                    } else {
                        //A script that does not call define(), so just simulate
                        //the call for it.
                        callGetModule([moduleName, (shim.deps || []), shim.exportsFn]);
                    }
                }

                checkLoaded();
            },

            /**
             * Converts a module name to a file path. Supports cases where
             * moduleName may actually be just an URL.
             * Note that it **does not** call normalize on the moduleName,
             * it is assumed to have already been normalized. This is an
             * internal API, not a public one. Use toUrl for the public API.
             */
            nameToUrl: function (moduleName, ext, skipExt) {
                var paths, syms, i, parentModule, url,
                    parentPath, bundleId,
                    pkgMain = getOwn(config.pkgs, moduleName);

                if (pkgMain) {
                    moduleName = pkgMain;
                }

                bundleId = getOwn(bundlesMap, moduleName);

                if (bundleId) {
                    return context.nameToUrl(bundleId, ext, skipExt);
                }

                //If a colon is in the URL, it indicates a protocol is used and it is just
                //an URL to a file, or if it starts with a slash, contains a query arg (i.e. ?)
                //or ends with .js, then assume the user meant to use an url and not a module id.
                //The slash is important for protocol-less URLs as well as full paths.
                if (req.jsExtRegExp.test(moduleName)) {
                    //Just a plain path, not module name lookup, so just return it.
                    //Add extension if it is included. This is a bit wonky, only non-.js things pass
                    //an extension, this method probably needs to be reworked.
                    url = moduleName + (ext || '');
                } else {
                    //A module that needs to be converted to a path.
                    paths = config.paths;

                    syms = moduleName.split('/');
                    //For each module name segment, see if there is a path
                    //registered for it. Start with most specific name
                    //and work up from it.
                    for (i = syms.length; i > 0; i -= 1) {
                        parentModule = syms.slice(0, i).join('/');

                        parentPath = getOwn(paths, parentModule);
                        if (parentPath) {
                            //If an array, it means there are a few choices,
                            //Choose the one that is desired
                            if (isArray(parentPath)) {
                                parentPath = parentPath[0];
                            }
                            syms.splice(0, i, parentPath);
                            break;
                        }
                    }

                    //Join the path parts together, then figure out if baseUrl is needed.
                    url = syms.join('/');
                    url += (ext || (/^data\:|\?/.test(url) || skipExt ? '' : '.js'));
                    url = (url.charAt(0) === '/' || url.match(/^[\w\+\.\-]+:/) ? '' : config.baseUrl) + url;
                }

                return config.urlArgs ? url +
                                        ((url.indexOf('?') === -1 ? '?' : '&') +
                                         config.urlArgs) : url;
            },

            //Delegates to req.load. Broken out as a separate function to
            //allow overriding in the optimizer.
            load: function (id, url) {
                req.load(context, id, url);
            },

            /**
             * Executes a module callback function. Broken out as a separate function
             * solely to allow the build system to sequence the files in the built
             * layer in the right sequence.
             *
             * @private
             */
            execCb: function (name, callback, args, exports) {
                return callback.apply(exports, args);
            },

            /**
             * callback for script loads, used to check status of loading.
             *
             * @param {Event} evt the event from the browser for the script
             * that was loaded.
             */
            onScriptLoad: function (evt) {
                //Using currentTarget instead of target for Firefox 2.0's sake. Not
                //all old browsers will be supported, but this one was easy enough
                //to support and still makes sense.
                if (evt.type === 'load' ||
                        (readyRegExp.test((evt.currentTarget || evt.srcElement).readyState))) {
                    //Reset interactive script so a script node is not held onto for
                    //to long.
                    interactiveScript = null;

                    //Pull out the name of the module and the context.
                    var data = getScriptData(evt);
                    context.completeLoad(data.id);
                }
            },

            /**
             * Callback for script errors.
             */
            onScriptError: function (evt) {
                var data = getScriptData(evt);
                if (!hasPathFallback(data.id)) {
                    return onError(makeError('scripterror', 'Script error for: ' + data.id, evt, [data.id]));
                }
            }
        };

        context.require = context.makeRequire();
        return context;
    }

    /**
     * Main entry point.
     *
     * If the only argument to require is a string, then the module that
     * is represented by that string is fetched for the appropriate context.
     *
     * If the first argument is an array, then it will be treated as an array
     * of dependency string names to fetch. An optional function callback can
     * be specified to execute when all of those dependencies are available.
     *
     * Make a local req variable to help Caja compliance (it assumes things
     * on a require that are not standardized), and to give a short
     * name for minification/local scope use.
     */
    req = requirejs = function (deps, callback, errback, optional) {

        //Find the right context, use default
        var context, config,
            contextName = defContextName;

        // Determine if have config object in the call.
        if (!isArray(deps) && typeof deps !== 'string') {
            // deps is a config object
            config = deps;
            if (isArray(callback)) {
                // Adjust args if there are dependencies
                deps = callback;
                callback = errback;
                errback = optional;
            } else {
                deps = [];
            }
        }

        if (config && config.context) {
            contextName = config.context;
        }

        context = getOwn(contexts, contextName);
        if (!context) {
            context = contexts[contextName] = req.s.newContext(contextName);
        }

        if (config) {
            context.configure(config);
        }

        return context.require(deps, callback, errback);
    };

    /**
     * Support require.config() to make it easier to cooperate with other
     * AMD loaders on globally agreed names.
     */
    req.config = function (config) {
        return req(config);
    };

    /**
     * Execute something after the current tick
     * of the event loop. Override for other envs
     * that have a better solution than setTimeout.
     * @param  {Function} fn function to execute later.
     */
    req.nextTick = typeof setTimeout !== 'undefined' ? function (fn) {
        setTimeout(fn, 4);
    } : function (fn) { fn(); };

    /**
     * Export require as a global, but only if it does not already exist.
     */
    if (!require) {
        require = req;
    }

    req.version = version;

    //Used to filter out dependencies that are already paths.
    req.jsExtRegExp = /^\/|:|\?|\.js$/;
    req.isBrowser = isBrowser;
    s = req.s = {
        contexts: contexts,
        newContext: newContext
    };

    //Create default context.
    req({});

    //Exports some context-sensitive methods on global require.
    each([
        'toUrl',
        'undef',
        'defined',
        'specified'
    ], function (prop) {
        //Reference from contexts instead of early binding to default context,
        //so that during builds, the latest instance of the default context
        //with its config gets used.
        req[prop] = function () {
            var ctx = contexts[defContextName];
            return ctx.require[prop].apply(ctx, arguments);
        };
    });

    if (isBrowser) {
        head = s.head = document.getElementsByTagName('head')[0];
        //If BASE tag is in play, using appendChild is a problem for IE6.
        //When that browser dies, this can be removed. Details in this jQuery bug:
        //http://dev.jquery.com/ticket/2709
        baseElement = document.getElementsByTagName('base')[0];
        if (baseElement) {
            head = s.head = baseElement.parentNode;
        }
    }

    /**
     * Any errors that require explicitly generates will be passed to this
     * function. Intercept/override it if you want custom error handling.
     * @param {Error} err the error object.
     */
    req.onError = defaultOnError;

    /**
     * Creates the node for the load command. Only used in browser envs.
     */
    req.createNode = function (config, moduleName, url) {
        var node = config.xhtml ?
                document.createElementNS('http://www.w3.org/1999/xhtml', 'html:script') :
                document.createElement('script');
        node.type = config.scriptType || 'text/javascript';
        node.charset = 'utf-8';
        node.async = true;
        return node;
    };

    /**
     * Does the request to load a module for the browser case.
     * Make this a separate function to allow other environments
     * to override it.
     *
     * @param {Object} context the require context to find state.
     * @param {String} moduleName the name of the module.
     * @param {Object} url the URL to the module.
     */
    req.load = function (context, moduleName, url) {
        var config = (context && context.config) || {},
            node;
        if (isBrowser) {
            //In the browser so use a script tag
            node = req.createNode(config, moduleName, url);

            node.setAttribute('data-requirecontext', context.contextName);
            node.setAttribute('data-requiremodule', moduleName);

            //Set up load listener. Test attachEvent first because IE9 has
            //a subtle issue in its addEventListener and script onload firings
            //that do not match the behavior of all other browsers with
            //addEventListener support, which fire the onload event for a
            //script right after the script execution. See:
            //https://connect.microsoft.com/IE/feedback/details/648057/script-onload-event-is-not-fired-immediately-after-script-execution
            //UNFORTUNATELY Opera implements attachEvent but does not follow the script
            //script execution mode.
            if (node.attachEvent &&
                    //Check if node.attachEvent is artificially added by custom script or
                    //natively supported by browser
                    //read https://github.com/jrburke/requirejs/issues/187
                    //if we can NOT find [native code] then it must NOT natively supported.
                    //in IE8, node.attachEvent does not have toString()
                    //Note the test for "[native code" with no closing brace, see:
                    //https://github.com/jrburke/requirejs/issues/273
                    !(node.attachEvent.toString && node.attachEvent.toString().indexOf('[native code') < 0) &&
                    !isOpera) {
                //Probably IE. IE (at least 6-8) do not fire
                //script onload right after executing the script, so
                //we cannot tie the anonymous define call to a name.
                //However, IE reports the script as being in 'interactive'
                //readyState at the time of the define call.
                useInteractive = true;

                node.attachEvent('onreadystatechange', context.onScriptLoad);
                //It would be great to add an error handler here to catch
                //404s in IE9+. However, onreadystatechange will fire before
                //the error handler, so that does not help. If addEventListener
                //is used, then IE will fire error before load, but we cannot
                //use that pathway given the connect.microsoft.com issue
                //mentioned above about not doing the 'script execute,
                //then fire the script load event listener before execute
                //next script' that other browsers do.
                //Best hope: IE10 fixes the issues,
                //and then destroys all installs of IE 6-9.
                //node.attachEvent('onerror', context.onScriptError);
            } else {
                node.addEventListener('load', context.onScriptLoad, false);
                node.addEventListener('error', context.onScriptError, false);
            }
            node.src = url;

            //For some cache cases in IE 6-8, the script executes before the end
            //of the appendChild execution, so to tie an anonymous define
            //call to the module name (which is stored on the node), hold on
            //to a reference to this node, but clear after the DOM insertion.
            currentlyAddingScript = node;
            if (baseElement) {
                head.insertBefore(node, baseElement);
            } else {
                head.appendChild(node);
            }
            currentlyAddingScript = null;

            return node;
        } else if (isWebWorker) {
            try {
                //In a web worker, use importScripts. This is not a very
                //efficient use of importScripts, importScripts will block until
                //its script is downloaded and evaluated. However, if web workers
                //are in play, the expectation that a build has been done so that
                //only one script needs to be loaded anyway. This may need to be
                //reevaluated if other use cases become common.
                importScripts(url);

                //Account for anonymous modules
                context.completeLoad(moduleName);
            } catch (e) {
                context.onError(makeError('importscripts',
                                'importScripts failed for ' +
                                    moduleName + ' at ' + url,
                                e,
                                [moduleName]));
            }
        }
    };

    function getInteractiveScript() {
        if (interactiveScript && interactiveScript.readyState === 'interactive') {
            return interactiveScript;
        }

        eachReverse(scripts(), function (script) {
            if (script.readyState === 'interactive') {
                return (interactiveScript = script);
            }
        });
        return interactiveScript;
    }

    //Look for a data-main script attribute, which could also adjust the baseUrl.
    if (isBrowser && !cfg.skipDataMain) {
        //Figure out baseUrl. Get it from the script tag with require.js in it.
        eachReverse(scripts(), function (script) {
            //Set the 'head' where we can append children by
            //using the script's parent.
            if (!head) {
                head = script.parentNode;
            }

            //Look for a data-main attribute to set main script for the page
            //to load. If it is there, the path to data main becomes the
            //baseUrl, if it is not already set.
            dataMain = script.getAttribute('data-main');
            if (dataMain) {
                //Preserve dataMain in case it is a path (i.e. contains '?')
                mainScript = dataMain;

                //Set final baseUrl if there is not already an explicit one.
                if (!cfg.baseUrl) {
                    //Pull off the directory of data-main for use as the
                    //baseUrl.
                    src = mainScript.split('/');
                    mainScript = src.pop();
                    subPath = src.length ? src.join('/')  + '/' : './';

                    cfg.baseUrl = subPath;
                }

                //Strip off any trailing .js since mainScript is now
                //like a module name.
                mainScript = mainScript.replace(jsSuffixRegExp, '');

                 //If mainScript is still a path, fall back to dataMain
                if (req.jsExtRegExp.test(mainScript)) {
                    mainScript = dataMain;
                }

                //Put the data-main script in the files to load.
                cfg.deps = cfg.deps ? cfg.deps.concat(mainScript) : [mainScript];

                return true;
            }
        });
    }

    /**
     * The function that handles definitions of modules. Differs from
     * require() in that a string for the module should be the first argument,
     * and the function to execute after dependencies are loaded should
     * return a value to define the module corresponding to the first argument's
     * name.
     */
    define = function (name, deps, callback) {
        var node, context;

        //Allow for anonymous modules
        if (typeof name !== 'string') {
            //Adjust args appropriately
            callback = deps;
            deps = name;
            name = null;
        }

        //This module may not have dependencies
        if (!isArray(deps)) {
            callback = deps;
            deps = null;
        }

        //If no name, and callback is a function, then figure out if it a
        //CommonJS thing with dependencies.
        if (!deps && isFunction(callback)) {
            deps = [];
            //Remove comments from the callback string,
            //look for require calls, and pull them into the dependencies,
            //but only if there are function args.
            if (callback.length) {
                callback
                    .toString()
                    .replace(commentRegExp, '')
                    .replace(cjsRequireRegExp, function (match, dep) {
                        deps.push(dep);
                    });

                //May be a CommonJS thing even without require calls, but still
                //could use exports, and module. Avoid doing exports and module
                //work though if it just needs require.
                //REQUIRES the function to expect the CommonJS variables in the
                //order listed below.
                deps = (callback.length === 1 ? ['require'] : ['require', 'exports', 'module']).concat(deps);
            }
        }

        //If in IE 6-8 and hit an anonymous define() call, do the interactive
        //work.
        if (useInteractive) {
            node = currentlyAddingScript || getInteractiveScript();
            if (node) {
                if (!name) {
                    name = node.getAttribute('data-requiremodule');
                }
                context = contexts[node.getAttribute('data-requirecontext')];
            }
        }

        //Always save off evaluating the def call until the script onload handler.
        //This allows multiple modules to be in a file without prematurely
        //tracing dependencies, and allows for anonymous module support,
        //where the module name is not known until the script onload event
        //occurs. If no context, use the global queue, and get it processed
        //in the onscript load callback.
        (context ? context.defQueue : globalDefQueue).push([name, deps, callback]);
    };

    define.amd = {
        jQuery: true
    };


    /**
     * Executes the text. Normally just uses eval, but can be modified
     * to use a better, environment-specific call. Only used for transpiling
     * loader plugins, not for plain JS modules.
     * @param {String} text the text to execute/evaluate.
     */
    req.exec = function (text) {
        /*jslint evil: true */
        return eval(text);
    };

    //Set up with config info.
    req(cfg);
}(this));
require.config({
    paths: {
        highcharts:'http://g.alicdn.com/aic/sdk/highcharts.src',
        highcharts_s:'http://g.alicdn.com/aic/sdk/standalone-framework.src',
        Q:'http://g.alicdn.com/aic/sdk/q',
        xscroll: 'http://g.alicdn.com/aic/sdk/xscroll/xscroll',
        infinite: 'http://g.alicdn.com/aic/sdk/xscroll/infinite',
        pullup: 'http://g.alicdn.com/aic/sdk/xscroll/pullup',
        pulldown: 'http://g.alicdn.com/aic/sdk/xscroll/pulldown',
        user: 'http://g.alicdn.com/aic/sdk/user',
        // cookbook: 'http://0.0.0.0/public/alink/cookbook/src/cookbook.js?t=8',
        cookbook: 'http://g.alicdn.com/aicdevices/cookbook/0.1.33/cookbook',
        'asevented':'http://g.alicdn.com/aic/sdk/asevented',
        'user-selector-s':'http://g.alicdn.com/aic/sdk/user-selector-s',
        'user-selector-m':'http://g.alicdn.com/aic/sdk/user-selector-m'
    },
    shim: {
        'highcharts':{
            exports:'Highcharts',
            deps:['highcharts_s']
        },
        'xscroll': {
            exports: 'XScroll',
            deps: ['pullup', 'infinite', 'pulldown']
        }
    }
});
require(['__sdk_main__'], function(){});
(function() {
	//UT\u6253\u70b9\u4f7f\u7528
	window.addEventListener('DOMContentLoaded', function() {
		g__DomElapsed__ = +new Date();
	});
})();
define("__sdk_init__", function(){});

/* Zepto v1.1.6 - zepto event ajax form ie - zeptojs.com/license */

var Zepto = (function() {
  var undefined, key, $, classList, emptyArray = [], slice = emptyArray.slice, filter = emptyArray.filter,
    document = window.document,
    elementDisplay = {}, classCache = {},
    cssNumber = { 'column-count': 1, 'columns': 1, 'font-weight': 1, 'line-height': 1,'opacity': 1, 'z-index': 1, 'zoom': 1 },
    fragmentRE = /^\s*<(\w+|!)[^>]*>/,
    singleTagRE = /^<(\w+)\s*\/?>(?:<\/\1>|)$/,
    tagExpanderRE = /<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/ig,
    rootNodeRE = /^(?:body|html)$/i,
    capitalRE = /([A-Z])/g,

    // special attributes that should be get/set via method calls
    methodAttributes = ['val', 'css', 'html', 'text', 'data', 'width', 'height', 'offset'],

    adjacencyOperators = [ 'after', 'prepend', 'before', 'append' ],
    table = document.createElement('table'),
    tableRow = document.createElement('tr'),
    containers = {
      'tr': document.createElement('tbody'),
      'tbody': table, 'thead': table, 'tfoot': table,
      'td': tableRow, 'th': tableRow,
      '*': document.createElement('div')
    },
    readyRE = /complete|loaded|interactive/,
    simpleSelectorRE = /^[\w-]*$/,
    class2type = {},
    toString = class2type.toString,
    zepto = {},
    camelize, uniq,
    tempParent = document.createElement('div'),
    propMap = {
      'tabindex': 'tabIndex',
      'readonly': 'readOnly',
      'for': 'htmlFor',
      'class': 'className',
      'maxlength': 'maxLength',
      'cellspacing': 'cellSpacing',
      'cellpadding': 'cellPadding',
      'rowspan': 'rowSpan',
      'colspan': 'colSpan',
      'usemap': 'useMap',
      'frameborder': 'frameBorder',
      'contenteditable': 'contentEditable'
    },
    isArray = Array.isArray ||
      function(object){ return object instanceof Array }

  zepto.matches = function(element, selector) {
    if (!selector || !element || element.nodeType !== 1) return false
    var matchesSelector = element.webkitMatchesSelector || element.mozMatchesSelector ||
                          element.oMatchesSelector || element.matchesSelector
    if (matchesSelector) return matchesSelector.call(element, selector)
    // fall back to performing a selector:
    var match, parent = element.parentNode, temp = !parent
    if (temp) (parent = tempParent).appendChild(element)
    match = ~zepto.qsa(parent, selector).indexOf(element)
    temp && tempParent.removeChild(element)
    return match
  }

  function type(obj) {
    return obj == null ? String(obj) :
      class2type[toString.call(obj)] || "object"
  }

  function isFunction(value) { return type(value) == "function" }
  function isWindow(obj)     { return obj != null && obj == obj.window }
  function isDocument(obj)   { return obj != null && obj.nodeType == obj.DOCUMENT_NODE }
  function isObject(obj)     { return type(obj) == "object" }
  function isPlainObject(obj) {
    return isObject(obj) && !isWindow(obj) && Object.getPrototypeOf(obj) == Object.prototype
  }
  function likeArray(obj) { return typeof obj.length == 'number' }

  function compact(array) { return filter.call(array, function(item){ return item != null }) }
  function flatten(array) { return array.length > 0 ? $.fn.concat.apply([], array) : array }
  camelize = function(str){ return str.replace(/-+(.)?/g, function(match, chr){ return chr ? chr.toUpperCase() : '' }) }
  function dasherize(str) {
    return str.replace(/::/g, '/')
           .replace(/([A-Z]+)([A-Z][a-z])/g, '$1_$2')
           .replace(/([a-z\d])([A-Z])/g, '$1_$2')
           .replace(/_/g, '-')
           .toLowerCase()
  }
  uniq = function(array){ return filter.call(array, function(item, idx){ return array.indexOf(item) == idx }) }

  function classRE(name) {
    return name in classCache ?
      classCache[name] : (classCache[name] = new RegExp('(^|\\s)' + name + '(\\s|$)'))
  }

  function maybeAddPx(name, value) {
    return (typeof value == "number" && !cssNumber[dasherize(name)]) ? value + "px" : value
  }

  function defaultDisplay(nodeName) {
    var element, display
    if (!elementDisplay[nodeName]) {
      element = document.createElement(nodeName)
      document.body.appendChild(element)
      display = getComputedStyle(element, '').getPropertyValue("display")
      element.parentNode.removeChild(element)
      display == "none" && (display = "block")
      elementDisplay[nodeName] = display
    }
    return elementDisplay[nodeName]
  }

  function children(element) {
    return 'children' in element ?
      slice.call(element.children) :
      $.map(element.childNodes, function(node){ if (node.nodeType == 1) return node })
  }

  // `$.zepto.fragment` takes a html string and an optional tag name
  // to generate DOM nodes nodes from the given html string.
  // The generated DOM nodes are returned as an array.
  // This function can be overriden in plugins for example to make
  // it compatible with browsers that don't support the DOM fully.
  zepto.fragment = function(html, name, properties) {
    var dom, nodes, container

    // A special case optimization for a single tag
    if (singleTagRE.test(html)) dom = $(document.createElement(RegExp.$1))

    if (!dom) {
      if (html.replace) html = html.replace(tagExpanderRE, "<$1></$2>")
      if (name === undefined) name = fragmentRE.test(html) && RegExp.$1
      if (!(name in containers)) name = '*'

      container = containers[name]
      container.innerHTML = '' + html
      dom = $.each(slice.call(container.childNodes), function(){
        container.removeChild(this)
      })
    }

    if (isPlainObject(properties)) {
      nodes = $(dom)
      $.each(properties, function(key, value) {
        if (methodAttributes.indexOf(key) > -1) nodes[key](value)
        else nodes.attr(key, value)
      })
    }

    return dom
  }

  // `$.zepto.Z` swaps out the prototype of the given `dom` array
  // of nodes with `$.fn` and thus supplying all the Zepto functions
  // to the array. Note that `__proto__` is not supported on Internet
  // Explorer. This method can be overriden in plugins.
  zepto.Z = function(dom, selector) {
    dom = dom || []
    dom.__proto__ = $.fn
    dom.selector = selector || ''
    return dom
  }

  // `$.zepto.isZ` should return `true` if the given object is a Zepto
  // collection. This method can be overriden in plugins.
  zepto.isZ = function(object) {
    return object instanceof zepto.Z
  }

  // `$.zepto.init` is Zepto's counterpart to jQuery's `$.fn.init` and
  // takes a CSS selector and an optional context (and handles various
  // special cases).
  // This method can be overriden in plugins.
  zepto.init = function(selector, context) {
    var dom
    // If nothing given, return an empty Zepto collection
    if (!selector) return zepto.Z()
    // Optimize for string selectors
    else if (typeof selector == 'string') {
      selector = selector.trim()
      // If it's a html fragment, create nodes from it
      // Note: In both Chrome 21 and Firefox 15, DOM error 12
      // is thrown if the fragment doesn't begin with <
      if (selector[0] == '<' && fragmentRE.test(selector))
        dom = zepto.fragment(selector, RegExp.$1, context), selector = null
      // If there's a context, create a collection on that context first, and select
      // nodes from there
      else if (context !== undefined) return $(context).find(selector)
      // If it's a CSS selector, use it to select nodes.
      else dom = zepto.qsa(document, selector)
    }
    // If a function is given, call it when the DOM is ready
    else if (isFunction(selector)) return $(document).ready(selector)
    // If a Zepto collection is given, just return it
    else if (zepto.isZ(selector)) return selector
    else {
      // normalize array if an array of nodes is given
      if (isArray(selector)) dom = compact(selector)
      // Wrap DOM nodes.
      else if (isObject(selector))
        dom = [selector], selector = null
      // If it's a html fragment, create nodes from it
      else if (fragmentRE.test(selector))
        dom = zepto.fragment(selector.trim(), RegExp.$1, context), selector = null
      // If there's a context, create a collection on that context first, and select
      // nodes from there
      else if (context !== undefined) return $(context).find(selector)
      // And last but no least, if it's a CSS selector, use it to select nodes.
      else dom = zepto.qsa(document, selector)
    }
    // create a new Zepto collection from the nodes found
    return zepto.Z(dom, selector)
  }

  // `$` will be the base `Zepto` object. When calling this
  // function just call `$.zepto.init, which makes the implementation
  // details of selecting nodes and creating Zepto collections
  // patchable in plugins.
  $ = function(selector, context){
    return zepto.init(selector, context)
  }

  function extend(target, source, deep) {
    for (key in source)
      if (deep && (isPlainObject(source[key]) || isArray(source[key]))) {
        if (isPlainObject(source[key]) && !isPlainObject(target[key]))
          target[key] = {}
        if (isArray(source[key]) && !isArray(target[key]))
          target[key] = []
        extend(target[key], source[key], deep)
      }
      else if (source[key] !== undefined) target[key] = source[key]
  }

  // Copy all but undefined properties from one or more
  // objects to the `target` object.
  $.extend = function(target){
    var deep, args = slice.call(arguments, 1)
    if (typeof target == 'boolean') {
      deep = target
      target = args.shift()
    }
    args.forEach(function(arg){ extend(target, arg, deep) })
    return target
  }

  // `$.zepto.qsa` is Zepto's CSS selector implementation which
  // uses `document.querySelectorAll` and optimizes for some special cases, like `#id`.
  // This method can be overriden in plugins.
  zepto.qsa = function(element, selector){
    var found,
        maybeID = selector[0] == '#',
        maybeClass = !maybeID && selector[0] == '.',
        nameOnly = maybeID || maybeClass ? selector.slice(1) : selector, // Ensure that a 1 char tag name still gets checked
        isSimple = simpleSelectorRE.test(nameOnly)
    return (isDocument(element) && isSimple && maybeID) ?
      ( (found = element.getElementById(nameOnly)) ? [found] : [] ) :
      (element.nodeType !== 1 && element.nodeType !== 9) ? [] :
      slice.call(
        isSimple && !maybeID ?
          maybeClass ? element.getElementsByClassName(nameOnly) : // If it's simple, it could be a class
          element.getElementsByTagName(selector) : // Or a tag
          element.querySelectorAll(selector) // Or it's not simple, and we need to query all
      )
  }

  function filtered(nodes, selector) {
    return selector == null ? $(nodes) : $(nodes).filter(selector)
  }

  $.contains = document.documentElement.contains ?
    function(parent, node) {
      return parent !== node && parent.contains(node)
    } :
    function(parent, node) {
      while (node && (node = node.parentNode))
        if (node === parent) return true
      return false
    }

  function funcArg(context, arg, idx, payload) {
    return isFunction(arg) ? arg.call(context, idx, payload) : arg
  }

  function setAttribute(node, name, value) {
    value == null ? node.removeAttribute(name) : node.setAttribute(name, value)
  }

  // access className property while respecting SVGAnimatedString
  function className(node, value){
    var klass = node.className || '',
        svg   = klass && klass.baseVal !== undefined

    if (value === undefined) return svg ? klass.baseVal : klass
    svg ? (klass.baseVal = value) : (node.className = value)
  }

  // "true"  => true
  // "false" => false
  // "null"  => null
  // "42"    => 42
  // "42.5"  => 42.5
  // "08"    => "08"
  // JSON    => parse if valid
  // String  => self
  function deserializeValue(value) {
    try {
      return value ?
        value == "true" ||
        ( value == "false" ? false :
          value == "null" ? null :
          +value + "" == value ? +value :
          /^[\[\{]/.test(value) ? $.parseJSON(value) :
          value )
        : value
    } catch(e) {
      return value
    }
  }

  $.type = type
  $.isFunction = isFunction
  $.isWindow = isWindow
  $.isArray = isArray
  $.isPlainObject = isPlainObject

  $.isEmptyObject = function(obj) {
    var name
    for (name in obj) return false
    return true
  }

  $.inArray = function(elem, array, i){
    return emptyArray.indexOf.call(array, elem, i)
  }

  $.camelCase = camelize
  $.trim = function(str) {
    return str == null ? "" : String.prototype.trim.call(str)
  }

  // plugin compatibility
  $.uuid = 0
  $.support = { }
  $.expr = { }

  $.map = function(elements, callback){
    var value, values = [], i, key
    if (likeArray(elements))
      for (i = 0; i < elements.length; i++) {
        value = callback(elements[i], i)
        if (value != null) values.push(value)
      }
    else
      for (key in elements) {
        value = callback(elements[key], key)
        if (value != null) values.push(value)
      }
    return flatten(values)
  }

  $.each = function(elements, callback){
    var i, key
    if (likeArray(elements)) {
      for (i = 0; i < elements.length; i++)
        if (callback.call(elements[i], i, elements[i]) === false) return elements
    } else {
      for (key in elements)
        if (callback.call(elements[key], key, elements[key]) === false) return elements
    }

    return elements
  }

  $.grep = function(elements, callback){
    return filter.call(elements, callback)
  }

  if (window.JSON) $.parseJSON = JSON.parse

  // Populate the class2type map
  $.each("Boolean Number String Function Array Date RegExp Object Error".split(" "), function(i, name) {
    class2type[ "[object " + name + "]" ] = name.toLowerCase()
  })

  // Define methods that will be available on all
  // Zepto collections
  $.fn = {
    // Because a collection acts like an array
    // copy over these useful array functions.
    forEach: emptyArray.forEach,
    reduce: emptyArray.reduce,
    push: emptyArray.push,
    sort: emptyArray.sort,
    indexOf: emptyArray.indexOf,
    concat: emptyArray.concat,

    // `map` and `slice` in the jQuery API work differently
    // from their array counterparts
    map: function(fn){
      return $($.map(this, function(el, i){ return fn.call(el, i, el) }))
    },
    slice: function(){
      return $(slice.apply(this, arguments))
    },

    ready: function(callback){
      // need to check if document.body exists for IE as that browser reports
      // document ready when it hasn't yet created the body element
      if (readyRE.test(document.readyState) && document.body) callback($)
      else document.addEventListener('DOMContentLoaded', function(){ callback($) }, false)
      return this
    },
    get: function(idx){
      return idx === undefined ? slice.call(this) : this[idx >= 0 ? idx : idx + this.length]
    },
    toArray: function(){ return this.get() },
    size: function(){
      return this.length
    },
    remove: function(){
      return this.each(function(){
        if (this.parentNode != null)
          this.parentNode.removeChild(this)
      })
    },
    each: function(callback){
      emptyArray.every.call(this, function(el, idx){
        return callback.call(el, idx, el) !== false
      })
      return this
    },
    filter: function(selector){
      if (isFunction(selector)) return this.not(this.not(selector))
      return $(filter.call(this, function(element){
        return zepto.matches(element, selector)
      }))
    },
    add: function(selector,context){
      return $(uniq(this.concat($(selector,context))))
    },
    is: function(selector){
      return this.length > 0 && zepto.matches(this[0], selector)
    },
    not: function(selector){
      var nodes=[]
      if (isFunction(selector) && selector.call !== undefined)
        this.each(function(idx){
          if (!selector.call(this,idx)) nodes.push(this)
        })
      else {
        var excludes = typeof selector == 'string' ? this.filter(selector) :
          (likeArray(selector) && isFunction(selector.item)) ? slice.call(selector) : $(selector)
        this.forEach(function(el){
          if (excludes.indexOf(el) < 0) nodes.push(el)
        })
      }
      return $(nodes)
    },
    has: function(selector){
      return this.filter(function(){
        return isObject(selector) ?
          $.contains(this, selector) :
          $(this).find(selector).size()
      })
    },
    eq: function(idx){
      return idx === -1 ? this.slice(idx) : this.slice(idx, + idx + 1)
    },
    first: function(){
      var el = this[0]
      return el && !isObject(el) ? el : $(el)
    },
    last: function(){
      var el = this[this.length - 1]
      return el && !isObject(el) ? el : $(el)
    },
    find: function(selector){
      var result, $this = this
      if (!selector) result = $()
      else if (typeof selector == 'object')
        result = $(selector).filter(function(){
          var node = this
          return emptyArray.some.call($this, function(parent){
            return $.contains(parent, node)
          })
        })
      else if (this.length == 1) result = $(zepto.qsa(this[0], selector))
      else result = this.map(function(){ return zepto.qsa(this, selector) })
      return result
    },
    closest: function(selector, context){
      var node = this[0], collection = false
      if (typeof selector == 'object') collection = $(selector)
      while (node && !(collection ? collection.indexOf(node) >= 0 : zepto.matches(node, selector)))
        node = node !== context && !isDocument(node) && node.parentNode
      return $(node)
    },
    parents: function(selector){
      var ancestors = [], nodes = this
      while (nodes.length > 0)
        nodes = $.map(nodes, function(node){
          if ((node = node.parentNode) && !isDocument(node) && ancestors.indexOf(node) < 0) {
            ancestors.push(node)
            return node
          }
        })
      return filtered(ancestors, selector)
    },
    parent: function(selector){
      return filtered(uniq(this.pluck('parentNode')), selector)
    },
    children: function(selector){
      return filtered(this.map(function(){ return children(this) }), selector)
    },
    contents: function() {
      return this.map(function() { return slice.call(this.childNodes) })
    },
    siblings: function(selector){
      return filtered(this.map(function(i, el){
        return filter.call(children(el.parentNode), function(child){ return child!==el })
      }), selector)
    },
    empty: function(){
      return this.each(function(){ this.innerHTML = '' })
    },
    // `pluck` is borrowed from Prototype.js
    pluck: function(property){
      return $.map(this, function(el){ return el[property] })
    },
    show: function(){
      return this.each(function(){
        this.style.display == "none" && (this.style.display = '')
        if (getComputedStyle(this, '').getPropertyValue("display") == "none")
          this.style.display = defaultDisplay(this.nodeName)
      })
    },
    replaceWith: function(newContent){
      return this.before(newContent).remove()
    },
    wrap: function(structure){
      var func = isFunction(structure)
      if (this[0] && !func)
        var dom   = $(structure).get(0),
            clone = dom.parentNode || this.length > 1

      return this.each(function(index){
        $(this).wrapAll(
          func ? structure.call(this, index) :
            clone ? dom.cloneNode(true) : dom
        )
      })
    },
    wrapAll: function(structure){
      if (this[0]) {
        $(this[0]).before(structure = $(structure))
        var children
        // drill down to the inmost element
        while ((children = structure.children()).length) structure = children.first()
        $(structure).append(this)
      }
      return this
    },
    wrapInner: function(structure){
      var func = isFunction(structure)
      return this.each(function(index){
        var self = $(this), contents = self.contents(),
            dom  = func ? structure.call(this, index) : structure
        contents.length ? contents.wrapAll(dom) : self.append(dom)
      })
    },
    unwrap: function(){
      this.parent().each(function(){
        $(this).replaceWith($(this).children())
      })
      return this
    },
    clone: function(){
      return this.map(function(){ return this.cloneNode(true) })
    },
    hide: function(){
      return this.css("display", "none")
    },
    toggle: function(setting){
      return this.each(function(){
        var el = $(this)
        ;(setting === undefined ? el.css("display") == "none" : setting) ? el.show() : el.hide()
      })
    },
    prev: function(selector){ return $(this.pluck('previousElementSibling')).filter(selector || '*') },
    next: function(selector){ return $(this.pluck('nextElementSibling')).filter(selector || '*') },
    html: function(html){
      return 0 in arguments ?
        this.each(function(idx){
          var originHtml = this.innerHTML
          $(this).empty().append( funcArg(this, html, idx, originHtml) )
        }) :
        (0 in this ? this[0].innerHTML : null)
    },
    text: function(text){
      return 0 in arguments ?
        this.each(function(idx){
          var newText = funcArg(this, text, idx, this.textContent)
          this.textContent = newText == null ? '' : ''+newText
        }) :
        (0 in this ? this[0].textContent : null)
    },
    attr: function(name, value){
      var result
      return (typeof name == 'string' && !(1 in arguments)) ?
        (!this.length || this[0].nodeType !== 1 ? undefined :
          (!(result = this[0].getAttribute(name)) && name in this[0]) ? this[0][name] : result
        ) :
        this.each(function(idx){
          if (this.nodeType !== 1) return
          if (isObject(name)) for (key in name) setAttribute(this, key, name[key])
          else setAttribute(this, name, funcArg(this, value, idx, this.getAttribute(name)))
        })
    },
    removeAttr: function(name){
      return this.each(function(){ this.nodeType === 1 && name.split(' ').forEach(function(attribute){
        setAttribute(this, attribute)
      }, this)})
    },
    prop: function(name, value){
      name = propMap[name] || name
      return (1 in arguments) ?
        this.each(function(idx){
          this[name] = funcArg(this, value, idx, this[name])
        }) :
        (this[0] && this[0][name])
    },
    data: function(name, value){
      var attrName = 'data-' + name.replace(capitalRE, '-$1').toLowerCase()

      var data = (1 in arguments) ?
        this.attr(attrName, value) :
        this.attr(attrName)

      return data !== null ? deserializeValue(data) : undefined
    },
    val: function(value){
      return 0 in arguments ?
        this.each(function(idx){
          this.value = funcArg(this, value, idx, this.value)
        }) :
        (this[0] && (this[0].multiple ?
           $(this[0]).find('option').filter(function(){ return this.selected }).pluck('value') :
           this[0].value)
        )
    },
    offset: function(coordinates){
      if (coordinates) return this.each(function(index){
        var $this = $(this),
            coords = funcArg(this, coordinates, index, $this.offset()),
            parentOffset = $this.offsetParent().offset(),
            props = {
              top:  coords.top  - parentOffset.top,
              left: coords.left - parentOffset.left
            }

        if ($this.css('position') == 'static') props['position'] = 'relative'
        $this.css(props)
      })
      if (!this.length) return null
      var obj = this[0].getBoundingClientRect()
      return {
        left: obj.left + window.pageXOffset,
        top: obj.top + window.pageYOffset,
        width: Math.round(obj.width),
        height: Math.round(obj.height)
      }
    },
    css: function(property, value){
      if (arguments.length < 2) {
        var computedStyle, element = this[0]
        if(!element) return
        computedStyle = getComputedStyle(element, '')
        if (typeof property == 'string')
          return element.style[camelize(property)] || computedStyle.getPropertyValue(property)
        else if (isArray(property)) {
          var props = {}
          $.each(property, function(_, prop){
            props[prop] = (element.style[camelize(prop)] || computedStyle.getPropertyValue(prop))
          })
          return props
        }
      }

      var css = ''
      if (type(property) == 'string') {
        if (!value && value !== 0)
          this.each(function(){ this.style.removeProperty(dasherize(property)) })
        else
          css = dasherize(property) + ":" + maybeAddPx(property, value)
      } else {
        for (key in property)
          if (!property[key] && property[key] !== 0)
            this.each(function(){ this.style.removeProperty(dasherize(key)) })
          else
            css += dasherize(key) + ':' + maybeAddPx(key, property[key]) + ';'
      }

      return this.each(function(){ this.style.cssText += ';' + css })
    },
    index: function(element){
      return element ? this.indexOf($(element)[0]) : this.parent().children().indexOf(this[0])
    },
    hasClass: function(name){
      if (!name) return false
      return emptyArray.some.call(this, function(el){
        return this.test(className(el))
      }, classRE(name))
    },
    addClass: function(name){
      if (!name) return this
      return this.each(function(idx){
        if (!('className' in this)) return
        classList = []
        var cls = className(this), newName = funcArg(this, name, idx, cls)
        newName.split(/\s+/g).forEach(function(klass){
          if (!$(this).hasClass(klass)) classList.push(klass)
        }, this)
        classList.length && className(this, cls + (cls ? " " : "") + classList.join(" "))
      })
    },
    removeClass: function(name){
      return this.each(function(idx){
        if (!('className' in this)) return
        if (name === undefined) return className(this, '')
        classList = className(this)
        funcArg(this, name, idx, classList).split(/\s+/g).forEach(function(klass){
          classList = classList.replace(classRE(klass), " ")
        })
        className(this, classList.trim())
      })
    },
    toggleClass: function(name, when){
      if (!name) return this
      return this.each(function(idx){
        var $this = $(this), names = funcArg(this, name, idx, className(this))
        names.split(/\s+/g).forEach(function(klass){
          (when === undefined ? !$this.hasClass(klass) : when) ?
            $this.addClass(klass) : $this.removeClass(klass)
        })
      })
    },
    scrollTop: function(value){
      if (!this.length) return
      var hasScrollTop = 'scrollTop' in this[0]
      if (value === undefined) return hasScrollTop ? this[0].scrollTop : this[0].pageYOffset
      return this.each(hasScrollTop ?
        function(){ this.scrollTop = value } :
        function(){ this.scrollTo(this.scrollX, value) })
    },
    scrollLeft: function(value){
      if (!this.length) return
      var hasScrollLeft = 'scrollLeft' in this[0]
      if (value === undefined) return hasScrollLeft ? this[0].scrollLeft : this[0].pageXOffset
      return this.each(hasScrollLeft ?
        function(){ this.scrollLeft = value } :
        function(){ this.scrollTo(value, this.scrollY) })
    },
    position: function() {
      if (!this.length) return

      var elem = this[0],
        // Get *real* offsetParent
        offsetParent = this.offsetParent(),
        // Get correct offsets
        offset       = this.offset(),
        parentOffset = rootNodeRE.test(offsetParent[0].nodeName) ? { top: 0, left: 0 } : offsetParent.offset()

      // Subtract element margins
      // note: when an element has margin: auto the offsetLeft and marginLeft
      // are the same in Safari causing offset.left to incorrectly be 0
      offset.top  -= parseFloat( $(elem).css('margin-top') ) || 0
      offset.left -= parseFloat( $(elem).css('margin-left') ) || 0

      // Add offsetParent borders
      parentOffset.top  += parseFloat( $(offsetParent[0]).css('border-top-width') ) || 0
      parentOffset.left += parseFloat( $(offsetParent[0]).css('border-left-width') ) || 0

      // Subtract the two offsets
      return {
        top:  offset.top  - parentOffset.top,
        left: offset.left - parentOffset.left
      }
    },
    offsetParent: function() {
      return this.map(function(){
        var parent = this.offsetParent || document.body
        while (parent && !rootNodeRE.test(parent.nodeName) && $(parent).css("position") == "static")
          parent = parent.offsetParent
        return parent
      })
    }
  }

  // for now
  $.fn.detach = $.fn.remove

  // Generate the `width` and `height` functions
  ;['width', 'height'].forEach(function(dimension){
    var dimensionProperty =
      dimension.replace(/./, function(m){ return m[0].toUpperCase() })

    $.fn[dimension] = function(value){
      var offset, el = this[0]
      if (value === undefined) return isWindow(el) ? el['inner' + dimensionProperty] :
        isDocument(el) ? el.documentElement['scroll' + dimensionProperty] :
        (offset = this.offset()) && offset[dimension]
      else return this.each(function(idx){
        el = $(this)
        el.css(dimension, funcArg(this, value, idx, el[dimension]()))
      })
    }
  })

  function traverseNode(node, fun) {
    fun(node)
    for (var i = 0, len = node.childNodes.length; i < len; i++)
      traverseNode(node.childNodes[i], fun)
  }

  // Generate the `after`, `prepend`, `before`, `append`,
  // `insertAfter`, `insertBefore`, `appendTo`, and `prependTo` methods.
  adjacencyOperators.forEach(function(operator, operatorIndex) {
    var inside = operatorIndex % 2 //=> prepend, append

    $.fn[operator] = function(){
      // arguments can be nodes, arrays of nodes, Zepto objects and HTML strings
      var argType, nodes = $.map(arguments, function(arg) {
            argType = type(arg)
            return argType == "object" || argType == "array" || arg == null ?
              arg : zepto.fragment(arg)
          }),
          parent, copyByClone = this.length > 1
      if (nodes.length < 1) return this

      return this.each(function(_, target){
        parent = inside ? target : target.parentNode

        // convert all methods to a "before" operation
        target = operatorIndex == 0 ? target.nextSibling :
                 operatorIndex == 1 ? target.firstChild :
                 operatorIndex == 2 ? target :
                 null

        var parentInDocument = $.contains(document.documentElement, parent)

        nodes.forEach(function(node){
          if (copyByClone) node = node.cloneNode(true)
          else if (!parent) return $(node).remove()

          parent.insertBefore(node, target)
          if (parentInDocument) traverseNode(node, function(el){
            if (el.nodeName != null && el.nodeName.toUpperCase() === 'SCRIPT' &&
               (!el.type || el.type === 'text/javascript') && !el.src)
              window['eval'].call(window, el.innerHTML)
          })
        })
      })
    }

    // after    => insertAfter
    // prepend  => prependTo
    // before   => insertBefore
    // append   => appendTo
    $.fn[inside ? operator+'To' : 'insert'+(operatorIndex ? 'Before' : 'After')] = function(html){
      $(html)[operator](this)
      return this
    }
  })

  zepto.Z.prototype = $.fn

  // Export internal API functions in the `$.zepto` namespace
  zepto.uniq = uniq
  zepto.deserializeValue = deserializeValue
  $.zepto = zepto

  return $
})()

window.Zepto = Zepto
window.$ === undefined && (window.$ = Zepto)

;(function($){
  var _zid = 1, undefined,
      slice = Array.prototype.slice,
      isFunction = $.isFunction,
      isString = function(obj){ return typeof obj == 'string' },
      handlers = {},
      specialEvents={},
      focusinSupported = 'onfocusin' in window,
      focus = { focus: 'focusin', blur: 'focusout' },
      hover = { mouseenter: 'mouseover', mouseleave: 'mouseout' }

  specialEvents.click = specialEvents.mousedown = specialEvents.mouseup = specialEvents.mousemove = 'MouseEvents'

  function zid(element) {
    return element._zid || (element._zid = _zid++)
  }
  function findHandlers(element, event, fn, selector) {
    event = parse(event)
    if (event.ns) var matcher = matcherFor(event.ns)
    return (handlers[zid(element)] || []).filter(function(handler) {
      return handler
        && (!event.e  || handler.e == event.e)
        && (!event.ns || matcher.test(handler.ns))
        && (!fn       || zid(handler.fn) === zid(fn))
        && (!selector || handler.sel == selector)
    })
  }
  function parse(event) {
    var parts = ('' + event).split('.')
    return {e: parts[0], ns: parts.slice(1).sort().join(' ')}
  }
  function matcherFor(ns) {
    return new RegExp('(?:^| )' + ns.replace(' ', ' .* ?') + '(?: |$)')
  }

  function eventCapture(handler, captureSetting) {
    return handler.del &&
      (!focusinSupported && (handler.e in focus)) ||
      !!captureSetting
  }

  function realEvent(type) {
    return hover[type] || (focusinSupported && focus[type]) || type
  }

  function add(element, events, fn, data, selector, delegator, capture){
    var id = zid(element), set = (handlers[id] || (handlers[id] = []))
    events.split(/\s/).forEach(function(event){
      if (event == 'ready') return $(document).ready(fn)
      var handler   = parse(event)
      handler.fn    = fn
      handler.sel   = selector
      // emulate mouseenter, mouseleave
      if (handler.e in hover) fn = function(e){
        var related = e.relatedTarget
        if (!related || (related !== this && !$.contains(this, related)))
          return handler.fn.apply(this, arguments)
      }
      handler.del   = delegator
      var callback  = delegator || fn
      handler.proxy = function(e){
        e = compatible(e)
        if (e.isImmediatePropagationStopped()) return
        e.data = data
        var result = callback.apply(element, e._args == undefined ? [e] : [e].concat(e._args))
        if (result === false) e.preventDefault(), e.stopPropagation()
        return result
      }
      handler.i = set.length
      set.push(handler)
      if ('addEventListener' in element)
        element.addEventListener(realEvent(handler.e), handler.proxy, eventCapture(handler, capture))
    })
  }
  function remove(element, events, fn, selector, capture){
    var id = zid(element)
    ;(events || '').split(/\s/).forEach(function(event){
      findHandlers(element, event, fn, selector).forEach(function(handler){
        delete handlers[id][handler.i]
      if ('removeEventListener' in element)
        element.removeEventListener(realEvent(handler.e), handler.proxy, eventCapture(handler, capture))
      })
    })
  }

  $.event = { add: add, remove: remove }

  $.proxy = function(fn, context) {
    var args = (2 in arguments) && slice.call(arguments, 2)
    if (isFunction(fn)) {
      var proxyFn = function(){ return fn.apply(context, args ? args.concat(slice.call(arguments)) : arguments) }
      proxyFn._zid = zid(fn)
      return proxyFn
    } else if (isString(context)) {
      if (args) {
        args.unshift(fn[context], fn)
        return $.proxy.apply(null, args)
      } else {
        return $.proxy(fn[context], fn)
      }
    } else {
      throw new TypeError("expected function")
    }
  }

  $.fn.bind = function(event, data, callback){
    return this.on(event, data, callback)
  }
  $.fn.unbind = function(event, callback){
    return this.off(event, callback)
  }
  $.fn.one = function(event, selector, data, callback){
    return this.on(event, selector, data, callback, 1)
  }

  var returnTrue = function(){return true},
      returnFalse = function(){return false},
      ignoreProperties = /^([A-Z]|returnValue$|layer[XY]$)/,
      eventMethods = {
        preventDefault: 'isDefaultPrevented',
        stopImmediatePropagation: 'isImmediatePropagationStopped',
        stopPropagation: 'isPropagationStopped'
      }

  function compatible(event, source) {
    if (source || !event.isDefaultPrevented) {
      source || (source = event)

      $.each(eventMethods, function(name, predicate) {
        var sourceMethod = source[name]
        event[name] = function(){
          this[predicate] = returnTrue
          return sourceMethod && sourceMethod.apply(source, arguments)
        }
        event[predicate] = returnFalse
      })

      if (source.defaultPrevented !== undefined ? source.defaultPrevented :
          'returnValue' in source ? source.returnValue === false :
          source.getPreventDefault && source.getPreventDefault())
        event.isDefaultPrevented = returnTrue
    }
    return event
  }

  function createProxy(event) {
    var key, proxy = { originalEvent: event }
    for (key in event)
      if (!ignoreProperties.test(key) && event[key] !== undefined) proxy[key] = event[key]

    return compatible(proxy, event)
  }

  $.fn.delegate = function(selector, event, callback){
    return this.on(event, selector, callback)
  }
  $.fn.undelegate = function(selector, event, callback){
    return this.off(event, selector, callback)
  }

  $.fn.live = function(event, callback){
    $(document.body).delegate(this.selector, event, callback)
    return this
  }
  $.fn.die = function(event, callback){
    $(document.body).undelegate(this.selector, event, callback)
    return this
  }

  $.fn.on = function(event, selector, data, callback, one){
    var autoRemove, delegator, $this = this
    if (event && !isString(event)) {
      $.each(event, function(type, fn){
        $this.on(type, selector, data, fn, one)
      })
      return $this
    }

    if (!isString(selector) && !isFunction(callback) && callback !== false)
      callback = data, data = selector, selector = undefined
    if (isFunction(data) || data === false)
      callback = data, data = undefined

    if (callback === false) callback = returnFalse

    return $this.each(function(_, element){
      if (one) autoRemove = function(e){
        remove(element, e.type, callback)
        return callback.apply(this, arguments)
      }

      if (selector) delegator = function(e){
        var evt, match = $(e.target).closest(selector, element).get(0)
        if (match && match !== element) {
          evt = $.extend(createProxy(e), {currentTarget: match, liveFired: element})
          return (autoRemove || callback).apply(match, [evt].concat(slice.call(arguments, 1)))
        }
      }

      add(element, event, callback, data, selector, delegator || autoRemove)
    })
  }
  $.fn.off = function(event, selector, callback){
    var $this = this
    if (event && !isString(event)) {
      $.each(event, function(type, fn){
        $this.off(type, selector, fn)
      })
      return $this
    }

    if (!isString(selector) && !isFunction(callback) && callback !== false)
      callback = selector, selector = undefined

    if (callback === false) callback = returnFalse

    return $this.each(function(){
      remove(this, event, callback, selector)
    })
  }

  $.fn.trigger = function(event, args){
    event = (isString(event) || $.isPlainObject(event)) ? $.Event(event) : compatible(event)
    event._args = args
    return this.each(function(){
      // handle focus(), blur() by calling them directly
      if (event.type in focus && typeof this[event.type] == "function") this[event.type]()
      // items in the collection might not be DOM elements
      else if ('dispatchEvent' in this) this.dispatchEvent(event)
      else $(this).triggerHandler(event, args)
    })
  }

  // triggers event handlers on current element just as if an event occurred,
  // doesn't trigger an actual event, doesn't bubble
  $.fn.triggerHandler = function(event, args){
    var e, result
    this.each(function(i, element){
      e = createProxy(isString(event) ? $.Event(event) : event)
      e._args = args
      e.target = element
      $.each(findHandlers(element, event.type || event), function(i, handler){
        result = handler.proxy(e)
        if (e.isImmediatePropagationStopped()) return false
      })
    })
    return result
  }

  // shortcut methods for `.bind(event, fn)` for each event type
  ;('focusin focusout focus blur load resize scroll unload click dblclick '+
  'mousedown mouseup mousemove mouseover mouseout mouseenter mouseleave '+
  'change select keydown keypress keyup error').split(' ').forEach(function(event) {
    $.fn[event] = function(callback) {
      return (0 in arguments) ?
        this.bind(event, callback) :
        this.trigger(event)
    }
  })

  $.Event = function(type, props) {
    if (!isString(type)) props = type, type = props.type
    var event = document.createEvent(specialEvents[type] || 'Events'), bubbles = true
    if (props) for (var name in props) (name == 'bubbles') ? (bubbles = !!props[name]) : (event[name] = props[name])
    event.initEvent(type, bubbles, true)
    return compatible(event)
  }

})(Zepto)

;(function($){
  var jsonpID = 0,
      document = window.document,
      key,
      name,
      rscript = /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi,
      scriptTypeRE = /^(?:text|application)\/javascript/i,
      xmlTypeRE = /^(?:text|application)\/xml/i,
      jsonType = 'application/json',
      htmlType = 'text/html',
      blankRE = /^\s*$/,
      originAnchor = document.createElement('a')

  originAnchor.href = window.location.href

  // trigger a custom event and return false if it was cancelled
  function triggerAndReturn(context, eventName, data) {
    var event = $.Event(eventName)
    $(context).trigger(event, data)
    return !event.isDefaultPrevented()
  }

  // trigger an Ajax "global" event
  function triggerGlobal(settings, context, eventName, data) {
    if (settings.global) return triggerAndReturn(context || document, eventName, data)
  }

  // Number of active Ajax requests
  $.active = 0

  function ajaxStart(settings) {
    if (settings.global && $.active++ === 0) triggerGlobal(settings, null, 'ajaxStart')
  }
  function ajaxStop(settings) {
    if (settings.global && !(--$.active)) triggerGlobal(settings, null, 'ajaxStop')
  }

  // triggers an extra global event "ajaxBeforeSend" that's like "ajaxSend" but cancelable
  function ajaxBeforeSend(xhr, settings) {
    var context = settings.context
    if (settings.beforeSend.call(context, xhr, settings) === false ||
        triggerGlobal(settings, context, 'ajaxBeforeSend', [xhr, settings]) === false)
      return false

    triggerGlobal(settings, context, 'ajaxSend', [xhr, settings])
  }
  function ajaxSuccess(data, xhr, settings, deferred) {
    var context = settings.context, status = 'success'
    settings.success.call(context, data, status, xhr)
    if (deferred) deferred.resolveWith(context, [data, status, xhr])
    triggerGlobal(settings, context, 'ajaxSuccess', [xhr, settings, data])
    ajaxComplete(status, xhr, settings)
  }
  // type: "timeout", "error", "abort", "parsererror"
  function ajaxError(error, type, xhr, settings, deferred) {
    var context = settings.context
    settings.error.call(context, xhr, type, error)
    if (deferred) deferred.rejectWith(context, [xhr, type, error])
    triggerGlobal(settings, context, 'ajaxError', [xhr, settings, error || type])
    ajaxComplete(type, xhr, settings)
  }
  // status: "success", "notmodified", "error", "timeout", "abort", "parsererror"
  function ajaxComplete(status, xhr, settings) {
    var context = settings.context
    settings.complete.call(context, xhr, status)
    triggerGlobal(settings, context, 'ajaxComplete', [xhr, settings])
    ajaxStop(settings)
  }

  // Empty function, used as default callback
  function empty() {}

  $.ajaxJSONP = function(options, deferred){
    if (!('type' in options)) return $.ajax(options)

    var _callbackName = options.jsonpCallback,
      callbackName = ($.isFunction(_callbackName) ?
        _callbackName() : _callbackName) || ('jsonp' + (++jsonpID)),
      script = document.createElement('script'),
      originalCallback = window[callbackName],
      responseData,
      abort = function(errorType) {
        $(script).triggerHandler('error', errorType || 'abort')
      },
      xhr = { abort: abort }, abortTimeout

    if (deferred) deferred.promise(xhr)

    $(script).on('load error', function(e, errorType){
      clearTimeout(abortTimeout)
      $(script).off().remove()

      if (e.type == 'error' || !responseData) {
        ajaxError(null, errorType || 'error', xhr, options, deferred)
      } else {
        ajaxSuccess(responseData[0], xhr, options, deferred)
      }

      window[callbackName] = originalCallback
      if (responseData && $.isFunction(originalCallback))
        originalCallback(responseData[0])

      originalCallback = responseData = undefined
    })

    if (ajaxBeforeSend(xhr, options) === false) {
      abort('abort')
      return xhr
    }

    window[callbackName] = function(){
      responseData = arguments
    }

    script.src = options.url.replace(/\?(.+)=\?/, '?$1=' + callbackName)
    document.head.appendChild(script)

    if (options.timeout > 0) abortTimeout = setTimeout(function(){
      abort('timeout')
    }, options.timeout)

    return xhr
  }

  $.ajaxSettings = {
    // Default type of request
    type: 'GET',
    // Callback that is executed before request
    beforeSend: empty,
    // Callback that is executed if the request succeeds
    success: empty,
    // Callback that is executed the the server drops error
    error: empty,
    // Callback that is executed on request complete (both: error and success)
    complete: empty,
    // The context for the callbacks
    context: null,
    // Whether to trigger "global" Ajax events
    global: true,
    // Transport
    xhr: function () {
      return new window.XMLHttpRequest()
    },
    // MIME types mapping
    // IIS returns Javascript as "application/x-javascript"
    accepts: {
      script: 'text/javascript, application/javascript, application/x-javascript',
      json:   jsonType,
      xml:    'application/xml, text/xml',
      html:   htmlType,
      text:   'text/plain'
    },
    // Whether the request is to another domain
    crossDomain: false,
    // Default timeout
    timeout: 0,
    // Whether data should be serialized to string
    processData: true,
    // Whether the browser should be allowed to cache GET responses
    cache: true
  }

  function mimeToDataType(mime) {
    if (mime) mime = mime.split(';', 2)[0]
    return mime && ( mime == htmlType ? 'html' :
      mime == jsonType ? 'json' :
      scriptTypeRE.test(mime) ? 'script' :
      xmlTypeRE.test(mime) && 'xml' ) || 'text'
  }

  function appendQuery(url, query) {
    if (query == '') return url
    return (url + '&' + query).replace(/[&?]{1,2}/, '?')
  }

  // serialize payload and append it to the URL for GET requests
  function serializeData(options) {
    if (options.processData && options.data && $.type(options.data) != "string")
      options.data = $.param(options.data, options.traditional)
    if (options.data && (!options.type || options.type.toUpperCase() == 'GET'))
      options.url = appendQuery(options.url, options.data), options.data = undefined
  }

  $.ajax = function(options){
    var settings = $.extend({}, options || {}),
        deferred = $.Deferred && $.Deferred(),
        urlAnchor
    for (key in $.ajaxSettings) if (settings[key] === undefined) settings[key] = $.ajaxSettings[key]

    ajaxStart(settings)

    if (!settings.crossDomain) {
      urlAnchor = document.createElement('a')
      urlAnchor.href = settings.url
      urlAnchor.href = urlAnchor.href
      settings.crossDomain = (originAnchor.protocol + '//' + originAnchor.host) !== (urlAnchor.protocol + '//' + urlAnchor.host)
    }

    if (!settings.url) settings.url = window.location.toString()
    serializeData(settings)

    var dataType = settings.dataType, hasPlaceholder = /\?.+=\?/.test(settings.url)
    if (hasPlaceholder) dataType = 'jsonp'

    if (settings.cache === false || (
         (!options || options.cache !== true) &&
         ('script' == dataType || 'jsonp' == dataType)
        ))
      settings.url = appendQuery(settings.url, '_=' + Date.now())

    if ('jsonp' == dataType) {
      if (!hasPlaceholder)
        settings.url = appendQuery(settings.url,
          settings.jsonp ? (settings.jsonp + '=?') : settings.jsonp === false ? '' : 'callback=?')
      return $.ajaxJSONP(settings, deferred)
    }

    var mime = settings.accepts[dataType],
        headers = { },
        setHeader = function(name, value) { headers[name.toLowerCase()] = [name, value] },
        protocol = /^([\w-]+:)\/\//.test(settings.url) ? RegExp.$1 : window.location.protocol,
        xhr = settings.xhr(),
        nativeSetHeader = xhr.setRequestHeader,
        abortTimeout

    if (deferred) deferred.promise(xhr)

    if (!settings.crossDomain) setHeader('X-Requested-With', 'XMLHttpRequest')
    setHeader('Accept', mime || '*/*')
    if (mime = settings.mimeType || mime) {
      if (mime.indexOf(',') > -1) mime = mime.split(',', 2)[0]
      xhr.overrideMimeType && xhr.overrideMimeType(mime)
    }
    if (settings.contentType || (settings.contentType !== false && settings.data && settings.type.toUpperCase() != 'GET'))
      setHeader('Content-Type', settings.contentType || 'application/x-www-form-urlencoded')

    if (settings.headers) for (name in settings.headers) setHeader(name, settings.headers[name])
    xhr.setRequestHeader = setHeader

    xhr.onreadystatechange = function(){
      if (xhr.readyState == 4) {
        xhr.onreadystatechange = empty
        clearTimeout(abortTimeout)
        var result, error = false
        if ((xhr.status >= 200 && xhr.status < 300) || xhr.status == 304 || (xhr.status == 0 && protocol == 'file:')) {
          dataType = dataType || mimeToDataType(settings.mimeType || xhr.getResponseHeader('content-type'))
          result = xhr.responseText

          try {
            // http://perfectionkills.com/global-eval-what-are-the-options/
            if (dataType == 'script')    (1,eval)(result)
            else if (dataType == 'xml')  result = xhr.responseXML
            else if (dataType == 'json') result = blankRE.test(result) ? null : $.parseJSON(result)
          } catch (e) { error = e }

          if (error) ajaxError(error, 'parsererror', xhr, settings, deferred)
          else ajaxSuccess(result, xhr, settings, deferred)
        } else {
          ajaxError(xhr.statusText || null, xhr.status ? 'error' : 'abort', xhr, settings, deferred)
        }
      }
    }

    if (ajaxBeforeSend(xhr, settings) === false) {
      xhr.abort()
      ajaxError(null, 'abort', xhr, settings, deferred)
      return xhr
    }

    if (settings.xhrFields) for (name in settings.xhrFields) xhr[name] = settings.xhrFields[name]

    var async = 'async' in settings ? settings.async : true
    xhr.open(settings.type, settings.url, async, settings.username, settings.password)

    for (name in headers) nativeSetHeader.apply(xhr, headers[name])

    if (settings.timeout > 0) abortTimeout = setTimeout(function(){
        xhr.onreadystatechange = empty
        xhr.abort()
        ajaxError(null, 'timeout', xhr, settings, deferred)
      }, settings.timeout)

    // avoid sending empty string (#319)
    xhr.send(settings.data ? settings.data : null)
    return xhr
  }

  // handle optional data/success arguments
  function parseArguments(url, data, success, dataType) {
    if ($.isFunction(data)) dataType = success, success = data, data = undefined
    if (!$.isFunction(success)) dataType = success, success = undefined
    return {
      url: url
    , data: data
    , success: success
    , dataType: dataType
    }
  }

  $.get = function(/* url, data, success, dataType */){
    return $.ajax(parseArguments.apply(null, arguments))
  }

  $.post = function(/* url, data, success, dataType */){
    var options = parseArguments.apply(null, arguments)
    options.type = 'POST'
    return $.ajax(options)
  }

  $.getJSON = function(/* url, data, success */){
    var options = parseArguments.apply(null, arguments)
    options.dataType = 'json'
    return $.ajax(options)
  }

  $.fn.load = function(url, data, success){
    if (!this.length) return this
    var self = this, parts = url.split(/\s/), selector,
        options = parseArguments(url, data, success),
        callback = options.success
    if (parts.length > 1) options.url = parts[0], selector = parts[1]
    options.success = function(response){
      self.html(selector ?
        $('<div>').html(response.replace(rscript, "")).find(selector)
        : response)
      callback && callback.apply(self, arguments)
    }
    $.ajax(options)
    return this
  }

  var escape = encodeURIComponent

  function serialize(params, obj, traditional, scope){
    var type, array = $.isArray(obj), hash = $.isPlainObject(obj)
    $.each(obj, function(key, value) {
      type = $.type(value)
      if (scope) key = traditional ? scope :
        scope + '[' + (hash || type == 'object' || type == 'array' ? key : '') + ']'
      // handle data in serializeArray() format
      if (!scope && array) params.add(value.name, value.value)
      // recurse into nested objects
      else if (type == "array" || (!traditional && type == "object"))
        serialize(params, value, traditional, key)
      else params.add(key, value)
    })
  }

  $.param = function(obj, traditional){
    var params = []
    params.add = function(key, value) {
      if ($.isFunction(value)) value = value()
      if (value == null) value = ""
      this.push(escape(key) + '=' + escape(value))
    }
    serialize(params, obj, traditional)
    return params.join('&').replace(/%20/g, '+')
  }
})(Zepto)

;(function($){
  $.fn.serializeArray = function() {
    var name, type, result = [],
      add = function(value) {
        if (value.forEach) return value.forEach(add)
        result.push({ name: name, value: value })
      }
    if (this[0]) $.each(this[0].elements, function(_, field){
      type = field.type, name = field.name
      if (name && field.nodeName.toLowerCase() != 'fieldset' &&
        !field.disabled && type != 'submit' && type != 'reset' && type != 'button' && type != 'file' &&
        ((type != 'radio' && type != 'checkbox') || field.checked))
          add($(field).val())
    })
    return result
  }

  $.fn.serialize = function(){
    var result = []
    this.serializeArray().forEach(function(elm){
      result.push(encodeURIComponent(elm.name) + '=' + encodeURIComponent(elm.value))
    })
    return result.join('&')
  }

  $.fn.submit = function(callback) {
    if (0 in arguments) this.bind('submit', callback)
    else if (this.length) {
      var event = $.Event('submit')
      this.eq(0).trigger(event)
      if (!event.isDefaultPrevented()) this.get(0).submit()
    }
    return this
  }

})(Zepto)

;(function($){
  // __proto__ doesn't exist on IE<11, so redefine
  // the Z function to use object extension instead
  if (!('__proto__' in {})) {
    $.extend($.zepto, {
      Z: function(dom, selector){
        dom = dom || []
        $.extend(dom, $.fn)
        dom.selector = selector || ''
        dom.__Z = true
        return dom
      },
      // this is a kludge but works
      isZ: function(object){
        return $.type(object) === 'array' && '__Z' in object
      }
    })
  }

  // getComputedStyle shouldn't freak out when called
  // without a valid element as argument
  try {
    getComputedStyle(undefined)
  } catch(e) {
    var nativeGetComputedStyle = getComputedStyle;
    window.getComputedStyle = function(element){
      try {
        return nativeGetComputedStyle(element)
      } catch(e) {
        return null
      }
    }
  }
})(Zepto)
;
$$ =Dom7 =$;
['width', 'height'].forEach(function(dimension) {
    var offset, Dimension = dimension.replace(/./, function(m) { return m[0].toUpperCase() });
    $.fn['outer' + Dimension] = function(margin) {
      var elem = this;
      if (elem) {
        var size = elem[dimension]();
        var sides = {'width': ['left', 'right'], 'height': ['top', 'bottom']};
        sides[dimension].forEach(function(side) {
          if (margin) size += parseInt(elem.css('margin-' + side), 10);
        });
        return size;
      } else {
        return null;
      }
    };
  });

define("zepto", (function (global) {
    return function () {
        var ret, fn;
        return ret || global.zepto;
    };
}(this)));

!function(a,b){function c(a,b){a=a.toString().split("."),b=b.toString().split(".");for(var c=0;c<a.length||c<b.length;c++){var d=parseInt(a[c],10),e=parseInt(b[c],10);if(window.isNaN(d)&&(d=0),window.isNaN(e)&&(e=0),e>d)return-1;if(d>e)return 1}return 0}function d(a,b){h&&c(i,"2.4.0")<0?setTimeout(function(){a&&a(b)},1):a&&a(b)}var e=a.document,f=a.navigator.userAgent,g=/iPhone|iPad|iPod/i.test(f),h=/Android/i.test(f),i=f.match(/(?:OS|Android)[\/\s](\d+[._]\d+(?:[._]\d+)?)/i),j=f.match(/WindVane[\/\s](\d+[._]\d+[._]\d+)/),k=Object.prototype.hasOwnProperty,l=b.windvane=a.WindVane||(a.WindVane={}),m=a.WindVane_Native,n={},o=1,p=[],q=3,r="hybrid",s="wv_hybrid",t="iframe_",u="suc_",v="err_",w="defer_",x="param_";i=i?(i[1]||"0.0.0").replace(/\_/g,"."):"0.0.0",j=j?(j[1]||"0.0.0").replace(/\_/g,"."):"0.0.0";var y={call:function(a,d,e,f,i,k){var l,n;return"number"==typeof arguments[arguments.length-1]&&(k=arguments[arguments.length-1]),"function"!=typeof f&&(f=null),"function"!=typeof i&&(i=null),b.promise&&(n=b.promise.deferred()),l=k>0?setTimeout(function(){y.onFailure(l,{ret:["WV_ERR::TIMEOUT"]})},k):z.getSid(),z.registerCall(l,f,i,n),h?c(j,"2.7.0")>=0?z.callMethodByPrompt(a,d,z.buildParam(e),l+""):m&&m.callMethod&&m.callMethod(a,d,z.buildParam(e),l+""):g&&z.callMethodByIframe(a,d,z.buildParam(e),l+""),n?n.promise():void 0},fireEvent:function(a,b,c){var d=e.createEvent("HTMLEvents");d.initEvent(a,!1,!0),!b&&c&&z.chunks[c]&&(b=z.chunks[c].join("")),d.param=z.parseParam(b),e.dispatchEvent(d)},getParam:function(a){return z.params[x+a]||""},setData:function(a,b){z.chunks[a]=z.chunks[a]||[],z.chunks[a].push(b)},onSuccess:function(a,b){clearTimeout(a);var c=z.unregisterCall(a),e=c.success,f=c.deferred;!b&&z.chunks[a]&&(b=z.chunks[a].join(""),delete z.chunks[a]);var g=z.parseParam(b);d(function(a){e&&e(a),f&&f.resolve(a)},g.value||g),z.onComplete(a)},onFailure:function(a,b){clearTimeout(a);var c=z.unregisterCall(a),e=c.failure,f=c.deferred;!b&&z.chunks[a]&&(b=z.chunks[a].join(""),delete z.chunks[a]);var g=z.parseParam(b);d(function(a){e&&e(a),f&&f.reject(a)},g.value||g),z.onComplete(a)}},z={params:{},chunks:{},buildParam:function(a){return a&&"object"==typeof a?JSON.stringify(a):a||""},parseParam:function(a){var b;if(a&&"string"==typeof a)try{b=JSON.parse(a)}catch(c){b={ret:["WV_ERR::PARAM_PARSE_ERROR"]}}else b=a||{};return b},getSid:function(){return Math.floor(Math.random()*(1<<50))+""+o++},registerCall:function(a,b,c,d){b&&(n[u+a]=b),c&&(n[v+a]=c),d&&(n[w+a]=d)},unregisterCall:function(a){var b=u+a,c=v+a,d=w+a,e={success:n[b],failure:n[c],deferred:n[d]};return delete n[b],delete n[c],e.deferred&&delete n[d],e},useIframe:function(a,b){var c=t+a,d=p.pop();d||(d=e.createElement("iframe"),d.setAttribute("frameborder","0"),d.style.cssText="width:0;height:0;border:0;display:none;"),d.setAttribute("id",c),d.setAttribute("src",b),d.parentNode||setTimeout(function(){e.body.appendChild(d)},5)},retrieveIframe:function(a){var b=t+a,c=e.querySelector("#"+b);p.length>=q?e.body.removeChild(c):p.push(c)},callMethodByIframe:function(a,b,c,d){var e=r+"://"+a+":"+d+"/"+b+"?"+c;this.params[x+d]=c,this.useIframe(d,e)},callMethodByPrompt:function(a,b,c,d){var e=r+"://"+a+":"+d+"/"+b+"?"+c,f=s+":";this.params[x+d]=c,window.prompt(e,f)},onComplete:function(a){g&&this.retrieveIframe(a),delete this.params[x+a]}};for(var A in y)k.call(l,A)||(l[A]=y[A])}(window,window.lib||(window.lib={}));
define("windvane", (function (global) {
    return function () {
        var ret, fn;
        return ret || global.windvane;
    };
}(this)));

/**
 * @license RequireJS text 2.0.9 Copyright (c) 2010-2012, The Dojo Foundation All Rights Reserved.
 * Available via the MIT or new BSD license.
 * see: http://github.com/requirejs/text for details
 */
/*jslint regexp: true */
/*global require, XMLHttpRequest, ActiveXObject,
  define, window, process, Packages,
  java, location, Components, FileUtils */

define('text',[],function () {
    
    var text, fs, Cc, Ci, xpcIsWindows,
        module = {},
        progIds = ['Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'],
        xmlRegExp = /^\s*<\?xml(\s)+version=[\'\"](\d)*.(\d)*[\'\"](\s)*\?>/im,
        bodyRegExp = /<body[^>]*>\s*([\s\S]+)\s*<\/body>/im,
        hasLocation = typeof location !== 'undefined' && location.href,
        defaultProtocol = hasLocation && location.protocol && location.protocol.replace(/\:/, ''),
        defaultHostName = hasLocation && location.hostname,
        defaultPort = hasLocation && (location.port || undefined),
        buildMap = {},
        masterConfig = (module.config && module.config()) || {};

    text = {
        version: '2.0.9',

        strip: function (content) {
            //Strips <?xml ...?> declarations so that external SVG and XML
            //documents can be added to a document without worry. Also, if the string
            //is an HTML document, only the part inside the body tag is returned.
            if (content) {
                content = content.replace(xmlRegExp, "");
                var matches = content.match(bodyRegExp);
                if (matches) {
                    content = matches[1];
                }
            } else {
                content = "";
            }
            return content;
        },

        jsEscape: function (content) {
            return content.replace(/(['\\])/g, '\\$1')
                .replace(/[\f]/g, "\\f")
                .replace(/[\b]/g, "\\b")
                .replace(/[\n]/g, "\\n")
                .replace(/[\t]/g, "\\t")
                .replace(/[\r]/g, "\\r")
                .replace(/[\u2028]/g, "\\u2028")
                .replace(/[\u2029]/g, "\\u2029");
        },

        createXhr: masterConfig.createXhr || function () {
            //Would love to dump the ActiveX crap in here. Need IE 6 to die first.
            var xhr, i, progId;
            if (typeof XMLHttpRequest !== "undefined") {
                return new XMLHttpRequest();
            } else if (typeof ActiveXObject !== "undefined") {
                for (i = 0; i < 3; i += 1) {
                    progId = progIds[i];
                    try {
                        xhr = new ActiveXObject(progId);
                    } catch (e) {}

                    if (xhr) {
                        progIds = [progId];  // so faster next time
                        break;
                    }
                }
            }

            return xhr;
        },

        /**
         * Parses a resource name into its component parts. Resource names
         * look like: module/name.ext!strip, where the !strip part is
         * optional.
         * @param {String} name the resource name
         * @returns {Object} with properties "moduleName", "ext" and "strip"
         * where strip is a boolean.
         */
        parseName: function (name) {
            var modName, ext, temp,
                strip = false,
                index = name.indexOf("."),
                isRelative = name.indexOf('./') === 0 ||
                             name.indexOf('../') === 0;

            if (index !== -1 && (!isRelative || index > 1)) {
                modName = name.substring(0, index);
                ext = name.substring(index + 1, name.length);
            } else {
                modName = name;
            }

            temp = ext || modName;
            index = temp.indexOf("!");
            if (index !== -1) {
                //Pull off the strip arg.
                strip = temp.substring(index + 1) === "strip";
                temp = temp.substring(0, index);
                if (ext) {
                    ext = temp;
                } else {
                    modName = temp;
                }
            }

            return {
                moduleName: modName,
                ext: ext,
                strip: strip
            };
        },

        xdRegExp: /^((\w+)\:)?\/\/([^\/\\]+)/,
        
        useCrossHTML: function(url){
            if(url.indexOf('.html') > -1){
                return true;
            }
            return false;
        },

        /**
         * Is an URL on another domain. Only works for browser use, returns
         * false in non-browser environments. Only used to know if an
         * optimized .js version of a text resource should be loaded
         * instead.
         * @param {String} url
         * @returns Boolean
         */
        useXhr: function (url, protocol, hostname, port) {
            var uProtocol, uHostName, uPort,
                match = text.xdRegExp.exec(url);
            if (!match) {
                return true;
            }
            uProtocol = match[2];
            uHostName = match[3];

            uHostName = uHostName.split(':');
            uPort = uHostName[1];
            uHostName = uHostName[0];

            return (!uProtocol || uProtocol === protocol) &&
                   (!uHostName || uHostName.toLowerCase() === hostname.toLowerCase()) &&
                   ((!uPort && !uHostName) || uPort === port);
        },

        finishLoad: function (name, strip, content, onLoad) {
            content = strip ? text.strip(content) : content;
            if (masterConfig.isBuild) {
                buildMap[name] = content;
            }
            onLoad(content);
        },

        load: function (name, req, onLoad, config) {
            //Name has format: some.module.filext!strip
            //The strip part is optional.
            //if strip is present, then that means only get the string contents
            //inside a body tag in an HTML string. For XML/SVG content it means
            //removing the <?xml ...?> declarations so the content can be inserted
            //into the current doc without problems.

            // Do not bother with the work if a build and text will
            // not be inlined.
            
            if (config.isBuild && !config.inlineText) {
                onLoad();
                return;
            }

            masterConfig.isBuild = config.isBuild;

            var parsed = text.parseName(name),
                nonStripName = parsed.moduleName +
                    (parsed.ext ? '.' + parsed.ext : ''),
                url = req.toUrl(nonStripName),
                useXhr = (masterConfig.useXhr) ||
                         text.useXhr;

            //Load the text. Use XHR if possible and in a browser.
            if (!hasLocation || useXhr(url, defaultProtocol, defaultHostName, defaultPort)) {
                text.get(url, function (content) {
                    text.finishLoad(name, parsed.strip, content, onLoad);
                }, function (err) {
                    if (onLoad.error) {
                        onLoad.error(err);
                    }
                });
            } else if(text.useCrossHTML(url)){
                htmlLoad(url, function(content){
                   text.finishLoad(parsed.moduleName + '.' + parsed.ext,
                                    parsed.strip, content.content, onLoad); 
                })
                
            }else{
                //Need to fetch the resource across domains. Assume
                //the resource has been optimized into a JS module. Fetch
                //by the module name + extension, but do not include the
                //!strip part to avoid file system issues.
                req([nonStripName], function (content) {
                    text.finishLoad(parsed.moduleName + '.' + parsed.ext,
                                    parsed.strip, content, onLoad);
                });
            }
        },

        write: function (pluginName, moduleName, write, config) {
            if (buildMap.hasOwnProperty(moduleName)) {
                var content = text.jsEscape(buildMap[moduleName]);
                write.asModule(pluginName + "!" + moduleName,
                               "define(function () { return '" +
                                   content +
                               "';});\n");
            }
        },

        writeFile: function (pluginName, moduleName, req, write, config) {
            var parsed = text.parseName(moduleName),
                extPart = parsed.ext ? '.' + parsed.ext : '',
                nonStripName = parsed.moduleName + extPart,
                //Use a '.js' file name so that it indicates it is a
                //script that can be loaded across domains.
                fileName = req.toUrl(parsed.moduleName + extPart) + '.js';

            //Leverage own load() method to load plugin value, but only
            //write out values that do not have the strip argument,
            //to avoid any potential issues with ! in file names.
            text.load(nonStripName, req, function (value) {
                //Use own write() method to construct full module value.
                //But need to create shell that translates writeFile's
                //write() to the right interface.
                var textWrite = function (contents) {
                    return write(fileName, contents);
                };
                textWrite.asModule = function (moduleName, contents) {
                    return write.asModule(moduleName, fileName, contents);
                };

                text.write(pluginName, nonStripName, textWrite, config);
            }, config);
        }
    };
    
    // <link> load method
   var htmlLoad = function(url, callback) {
       // require(['Infrastructure'], function(Infrastructure){
       //     Infrastructure.alinkSDK.getRemoteContent(url, callback);
       // })
        
    }

    if (masterConfig.env === 'node' || (!masterConfig.env &&
            typeof process !== "undefined" &&
            process.versions &&
            !!process.versions.node &&
            !process.versions['node-webkit'])) {
        //Using special require.nodeRequire, something added by r.js.
        fs = require.nodeRequire('fs');

        text.get = function (url, callback, errback) {
            try {
                var file = fs.readFileSync(url, 'utf8');
                //Remove BOM (Byte Mark Order) from utf8 files if it is there.
                if (file.indexOf('\uFEFF') === 0) {
                    file = file.substring(1);
                }
                callback(file);
            } catch (e) {
                errback(e);
            }
        };
    } else if (masterConfig.env === 'xhr' || (!masterConfig.env &&
            text.createXhr())) {
        text.get = function (url, callback, errback, headers) {
            var xhr = text.createXhr(), header;
            xhr.open('GET', url, true);

            //Allow plugins direct access to xhr headers
            if (headers) {
                for (header in headers) {
                    if (headers.hasOwnProperty(header)) {
                        xhr.setRequestHeader(header.toLowerCase(), headers[header]);
                    }
                }
            }

            //Allow overrides specified in config
            if (masterConfig.onXhr) {
                masterConfig.onXhr(xhr, url);
            }

            xhr.onreadystatechange = function (evt) {
                var status, err;
                //Do not explicitly handle errors, those should be
                //visible via console output in the browser.
                if (xhr.readyState === 4) {
                    status = xhr.status;
                    if (status > 399 && status < 600) {
                        //An http 4xx or 5xx error. Signal an error.
                        err = new Error(url + ' HTTP status: ' + status);
                        err.xhr = xhr;
                        errback(err);
                    } else {
                        callback(xhr.responseText);
                    }

                    if (masterConfig.onXhrComplete) {
                        masterConfig.onXhrComplete(xhr, url);
                    }
                }
            };
            xhr.send(null);
        };
    } else if (masterConfig.env === 'rhino' || (!masterConfig.env &&
            typeof Packages !== 'undefined' && typeof java !== 'undefined')) {
        //Why Java, why is this so awkward?
        text.get = function (url, callback) {
            var stringBuffer, line,
                encoding = "utf-8",
                file = new java.io.File(url),
                lineSeparator = java.lang.System.getProperty("line.separator"),
                input = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), encoding)),
                content = '';
            try {
                stringBuffer = new java.lang.StringBuffer();
                line = input.readLine();

                // Byte Order Mark (BOM) - The Unicode Standard, version 3.0, page 324
                // http://www.unicode.org/faq/utf_bom.html

                // Note that when we use utf-8, the BOM should appear as "EF BB BF", but it doesn't due to this bug in the JDK:
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058
                if (line && line.length() && line.charAt(0) === 0xfeff) {
                    // Eat the BOM, since we've already found the encoding on this file,
                    // and we plan to concatenating this buffer with others; the BOM should
                    // only appear at the top of a file.
                    line = line.substring(1);
                }

                if (line !== null) {
                    stringBuffer.append(line);
                }

                while ((line = input.readLine()) !== null) {
                    stringBuffer.append(lineSeparator);
                    stringBuffer.append(line);
                }
                //Make sure we return a JavaScript string and not a Java string.
                content = String(stringBuffer.toString()); //String
            } finally {
                input.close();
            }
            callback(content);
        };
    } else if (masterConfig.env === 'xpconnect' || (!masterConfig.env &&
            typeof Components !== 'undefined' && Components.classes &&
            Components.interfaces)) {
        //Avert your gaze!
        Cc = Components.classes,
        Ci = Components.interfaces;
        Components.utils['import']('resource://gre/modules/FileUtils.jsm');
        xpcIsWindows = ('@mozilla.org/windows-registry-key;1' in Cc);

        text.get = function (url, callback) {
            var inStream, convertStream, fileObj,
                readData = {};

            if (xpcIsWindows) {
                url = url.replace(/\//g, '\\');
            }

            fileObj = new FileUtils.File(url);

            //XPCOM, you so crazy
            try {
                inStream = Cc['@mozilla.org/network/file-input-stream;1']
                           .createInstance(Ci.nsIFileInputStream);
                inStream.init(fileObj, 1, 0, false);

                convertStream = Cc['@mozilla.org/intl/converter-input-stream;1']
                                .createInstance(Ci.nsIConverterInputStream);
                convertStream.init(inStream, "utf-8", inStream.available(),
                Ci.nsIConverterInputStream.DEFAULT_REPLACEMENT_CHARACTER);

                convertStream.readString(inStream.available(), readData);
                convertStream.close();
                inStream.close();
                callback(readData.value);
            } catch (e) {
                throw new Error((fileObj && fileObj.path || '') + ': ' + e);
            }
        };
    }
    return text;
});
/*
 * Require-CSS RequireJS css! loader plugin
 * 0.1.2
 * Guy Bedford 2013
 * MIT
 */

/*
 *
 * Usage:
 *  require(['css!./mycssFile']);
 *
 * Tested and working in (up to latest versions as of March 2013):
 * Android
 * iOS 6
 * IE 6 - 10
 * Chome 3 - 26
 * Firefox 3.5 - 19
 * Opera 10 - 12
 *
 * browserling.com used for virtual testing environment
 *
 * Credit to B Cavalier & J Hann for the IE 6 - 9 method,
 * refined with help from Martin Cermak
 *
 * Sources that helped along the way:
 * - https://developer.mozilla.org/en-US/docs/Browser_detection_using_the_user_agent
 * - http://www.phpied.com/when-is-a-stylesheet-really-loaded/
 * - https://github.com/cujojs/curl/blob/master/src/curl/plugin/css.js
 *
 */

define('css',[],function() {
    if (typeof window == 'undefined')
        return { load: function(n, r, load){ load() } };

    var head = document.getElementsByTagName('head')[0];

    var engine = window.navigator.userAgent.match(/Trident\/([^ ;]*)|AppleWebKit\/([^ ;]*)|Opera\/([^ ;]*)|rv\:([^ ;]*)(.*?)Gecko\/([^ ;]*)|MSIE\s([^ ;]*)|AndroidWebKit\/([^ ;]*)/) || 0;

    // use <style> @import load method (IE < 9, Firefox < 18)
    var useImportLoad = false;

    // set to false for explicit <link> load checking when onload doesn't work perfectly (webkit)
    var useOnload = true;

    // trident / msie
    if (engine[1] || engine[7])
        useImportLoad = parseInt(engine[1]) < 6 || parseInt(engine[7]) <= 9;
    // webkit
    else if (engine[2] || engine[8])
        useOnload = false;
    // gecko
    else if (engine[4])
        useImportLoad = parseInt(engine[4]) < 18;

    
    
    //main api object
    var cssAPI = {};
    cssAPI.pluginBuilder = './css-builder';


    // <style> @import load method
    var curStyle, curSheet;
    var createStyle = function () {
        curStyle = document.createElement('style');
        head.appendChild(curStyle);
        curSheet = curStyle.styleSheet || curStyle.sheet;
    }
    var ieCnt = 0;
    var ieLoads = [];
    var ieCurCallback;

    var createIeLoad = function(url) {
        ieCnt++;
        if (ieCnt == 32) {
            createStyle();
            ieCnt = 0;
        }
        curSheet.addImport(url);
        curStyle.onload = function(){ processIeLoad() };
    }
    var processIeLoad = function() {
        ieCurCallback();

        var nextLoad = ieLoads.shift();

        if (!nextLoad) {
            ieCurCallback = null;
            return;
        }

        ieCurCallback = nextLoad[1];
        createIeLoad(nextLoad[0]);
    }
    var importLoad = function(url, callback) {
        if (!curSheet || !curSheet.addImport)
            createStyle();

        if (curSheet && curSheet.addImport) {
            // old IE
            if (ieCurCallback) {
                ieLoads.push([url, callback]);
            }
            else {
                createIeLoad(url);
                ieCurCallback = callback;
            }
        }
        else {
            // old Firefox
            curStyle.textContent = '@import "' + url + '";';

            var loadInterval = setInterval(function() {
                try {
                    curStyle.sheet.cssRules;
                    clearInterval(loadInterval);
                    callback();
                } catch(e) {}
            }, 10);
        }
    }

    // <link> load method
    var linkLoad = function(url, callback) {
        var link = document.createElement('link');
        link.type = 'text/css';
        link.rel = 'stylesheet';
        if (useOnload)
            link.onload = function() {
                link.onload = function() {};
                // for style dimensions queries, a short delay can still be necessary
                setTimeout(callback, 7);
            }
        else
            var loadInterval = setInterval(function() {
                for (var i = 0; i < document.styleSheets.length; i++) {
                    var sheet = document.styleSheets[i];
                    if (sheet.href == link.href) {
                        clearInterval(loadInterval);
                        return callback();
                    }
                }
            }, 10);
        link.href = url;
        head.appendChild(link);
    }

    cssAPI.normalize = function(name, normalize) {
        if (name.substr(name.length - 4, 4) == '.css')
            name = name.substr(0, name.length - 4);

        return normalize(name);
    }

    cssAPI.load = function(cssId, req, load, config) {
        (useImportLoad ? importLoad : linkLoad)(req.toUrl(cssId + '.css'), load);

    }

    return cssAPI;
});
;(function () {
	

	/**
	 * @preserve FastClick: polyfill to remove click delays on browsers with touch UIs.
	 *
	 * @codingstandard ftlabs-jsv2
	 * @copyright The Financial Times Limited [All Rights Reserved]
	 * @license MIT License (see LICENSE.txt)
	 */

	/*jslint browser:true, node:true*/
	/*global define, Event, Node*/


	/**
	 * Instantiate fast-clicking listeners on the specified layer.
	 *
	 * @constructor
	 * @param {Element} layer The layer to listen on
	 * @param {Object} [options={}] The options to override the defaults
	 */
	function FastClick(layer, options) {
		var oldOnClick;

		options = options || {};

		/**
		 * Whether a click is currently being tracked.
		 *
		 * @type boolean
		 */
		this.trackingClick = false;


		/**
		 * Timestamp for when click tracking started.
		 *
		 * @type number
		 */
		this.trackingClickStart = 0;


		/**
		 * The element being tracked for a click.
		 *
		 * @type EventTarget
		 */
		this.targetElement = null;


		/**
		 * X-coordinate of touch start event.
		 *
		 * @type number
		 */
		this.touchStartX = 0;


		/**
		 * Y-coordinate of touch start event.
		 *
		 * @type number
		 */
		this.touchStartY = 0;


		/**
		 * ID of the last touch, retrieved from Touch.identifier.
		 *
		 * @type number
		 */
		this.lastTouchIdentifier = 0;


		/**
		 * Touchmove boundary, beyond which a click will be cancelled.
		 *
		 * @type number
		 */
		this.touchBoundary = options.touchBoundary || 10;


		/**
		 * The FastClick layer.
		 *
		 * @type Element
		 */
		this.layer = layer;

		/**
		 * The minimum time between tap(touchstart and touchend) events
		 *
		 * @type number
		 */
		this.tapDelay = options.tapDelay || 200;

		/**
		 * The maximum time for a tap
		 *
		 * @type number
		 */
		this.tapTimeout = options.tapTimeout || 700;

		if (FastClick.notNeeded(layer)) {
			return;
		}

		// Some old versions of Android don't have Function.prototype.bind
		function bind(method, context) {
			return function() { return method.apply(context, arguments); };
		}


		var methods = ['onMouse', 'onClick', 'onTouchStart', 'onTouchMove', 'onTouchEnd', 'onTouchCancel'];
		var context = this;
		for (var i = 0, l = methods.length; i < l; i++) {
			context[methods[i]] = bind(context[methods[i]], context);
		}

		// Set up event handlers as required
		if (deviceIsAndroid) {
			layer.addEventListener('mouseover', this.onMouse, true);
			layer.addEventListener('mousedown', this.onMouse, true);
			layer.addEventListener('mouseup', this.onMouse, true);
		}

		layer.addEventListener('click', this.onClick, true);
		layer.addEventListener('touchstart', this.onTouchStart, false);
		layer.addEventListener('touchmove', this.onTouchMove, false);
		layer.addEventListener('touchend', this.onTouchEnd, false);
		layer.addEventListener('touchcancel', this.onTouchCancel, false);

		// Hack is required for browsers that don't support Event#stopImmediatePropagation (e.g. Android 2)
		// which is how FastClick normally stops click events bubbling to callbacks registered on the FastClick
		// layer when they are cancelled.
		if (!Event.prototype.stopImmediatePropagation) {
			layer.removeEventListener = function(type, callback, capture) {
				var rmv = Node.prototype.removeEventListener;
				if (type === 'click') {
					rmv.call(layer, type, callback.hijacked || callback, capture);
				} else {
					rmv.call(layer, type, callback, capture);
				}
			};

			layer.addEventListener = function(type, callback, capture) {
				var adv = Node.prototype.addEventListener;
				if (type === 'click') {
					adv.call(layer, type, callback.hijacked || (callback.hijacked = function(event) {
						if (!event.propagationStopped) {
							callback(event);
						}
					}), capture);
				} else {
					adv.call(layer, type, callback, capture);
				}
			};
		}

		// If a handler is already declared in the element's onclick attribute, it will be fired before
		// FastClick's onClick handler. Fix this by pulling out the user-defined handler function and
		// adding it as listener.
		if (typeof layer.onclick === 'function') {

			// Android browser on at least 3.2 requires a new reference to the function in layer.onclick
			// - the old one won't work if passed to addEventListener directly.
			oldOnClick = layer.onclick;
			layer.addEventListener('click', function(event) {
				oldOnClick(event);
			}, false);
			layer.onclick = null;
		}
	}

	/**
	* Windows Phone 8.1 fakes user agent string to look like Android and iPhone.
	*
	* @type boolean
	*/
	var deviceIsWindowsPhone = navigator.userAgent.indexOf("Windows Phone") >= 0;

	/**
	 * Android requires exceptions.
	 *
	 * @type boolean
	 */
	var deviceIsAndroid = navigator.userAgent.indexOf('Android') > 0 && !deviceIsWindowsPhone;


	/**
	 * iOS requires exceptions.
	 *
	 * @type boolean
	 */
	var deviceIsIOS = /iP(ad|hone|od)/.test(navigator.userAgent) && !deviceIsWindowsPhone;


	/**
	 * iOS 4 requires an exception for select elements.
	 *
	 * @type boolean
	 */
	var deviceIsIOS4 = deviceIsIOS && (/OS 4_\d(_\d)?/).test(navigator.userAgent);


	/**
	 * iOS 6.0-7.* requires the target element to be manually derived
	 *
	 * @type boolean
	 */
	var deviceIsIOSWithBadTarget = deviceIsIOS && (/OS [6-7]_\d/).test(navigator.userAgent);

	/**
	 * BlackBerry requires exceptions.
	 *
	 * @type boolean
	 */
	var deviceIsBlackBerry10 = navigator.userAgent.indexOf('BB10') > 0;

	/**
	 * Determine whether a given element requires a native click.
	 *
	 * @param {EventTarget|Element} target Target DOM element
	 * @returns {boolean} Returns true if the element needs a native click
	 */
	FastClick.prototype.needsClick = function(target) {
		switch (target.nodeName.toLowerCase()) {

		// Don't send a synthetic click to disabled inputs (issue #62)
		case 'button':
		case 'select':
		case 'textarea':
			if (target.disabled) {
				return true;
			}

			break;
		case 'input':

			// File inputs need real clicks on iOS 6 due to a browser bug (issue #68)
			if ((deviceIsIOS && target.type === 'file') || target.disabled) {
				return true;
			}

			break;
		case 'label':
		case 'iframe': // iOS8 homescreen apps can prevent events bubbling into frames
		case 'video':
			return true;
		}

		return (/\bneedsclick\b/).test(target.className);
	};


	/**
	 * Determine whether a given element requires a call to focus to simulate click into element.
	 *
	 * @param {EventTarget|Element} target Target DOM element
	 * @returns {boolean} Returns true if the element requires a call to focus to simulate native click.
	 */
	FastClick.prototype.needsFocus = function(target) {
		switch (target.nodeName.toLowerCase()) {
		case 'textarea':
			return true;
		case 'select':
			return !deviceIsAndroid;
		case 'input':
			switch (target.type) {
			case 'button':
			case 'checkbox':
			case 'file':
			case 'image':
			case 'radio':
			case 'submit':
				return false;
			}

			// No point in attempting to focus disabled inputs
			return !target.disabled && !target.readOnly;
		default:
			return (/\bneedsfocus\b/).test(target.className);
		}
	};


	/**
	 * Send a click event to the specified element.
	 *
	 * @param {EventTarget|Element} targetElement
	 * @param {Event} event
	 */
	FastClick.prototype.sendClick = function(targetElement, event) {
		var clickEvent, touch;

		// On some Android devices activeElement needs to be blurred otherwise the synthetic click will have no effect (#24)
		if (document.activeElement && document.activeElement !== targetElement) {
			document.activeElement.blur();
		}

		touch = event.changedTouches[0];

		// Synthesise a click event, with an extra attribute so it can be tracked
		clickEvent = document.createEvent('MouseEvents');
		clickEvent.initMouseEvent(this.determineEventType(targetElement), true, true, window, 1, touch.screenX, touch.screenY, touch.clientX, touch.clientY, false, false, false, false, 0, null);
		clickEvent.forwardedTouchEvent = true;
		targetElement.dispatchEvent(clickEvent);
	};

	FastClick.prototype.determineEventType = function(targetElement) {

		//Issue #159: Android Chrome Select Box does not open with a synthetic click event
		if (deviceIsAndroid && targetElement.tagName.toLowerCase() === 'select') {
			return 'mousedown';
		}

		return 'click';
	};


	/**
	 * @param {EventTarget|Element} targetElement
	 */
	FastClick.prototype.focus = function(targetElement) {
		var length;

		// Issue #160: on iOS 7, some input elements (e.g. date datetime month) throw a vague TypeError on setSelectionRange. These elements don't have an integer value for the selectionStart and selectionEnd properties, but unfortunately that can't be used for detection because accessing the properties also throws a TypeError. Just check the type instead. Filed as Apple bug #15122724.
		if (deviceIsIOS && targetElement.setSelectionRange && targetElement.type.indexOf('date') !== 0 && targetElement.type !== 'time' && targetElement.type !== 'month') {
			length = targetElement.value.length;
			targetElement.setSelectionRange(length, length);
		} else {
			targetElement.focus();
		}
	};


	/**
	 * Check whether the given target element is a child of a scrollable layer and if so, set a flag on it.
	 *
	 * @param {EventTarget|Element} targetElement
	 */
	FastClick.prototype.updateScrollParent = function(targetElement) {
		var scrollParent, parentElement;

		scrollParent = targetElement.fastClickScrollParent;

		// Attempt to discover whether the target element is contained within a scrollable layer. Re-check if the
		// target element was moved to another parent.
		if (!scrollParent || !scrollParent.contains(targetElement)) {
			parentElement = targetElement;
			do {
				if (parentElement.scrollHeight > parentElement.offsetHeight) {
					scrollParent = parentElement;
					targetElement.fastClickScrollParent = parentElement;
					break;
				}

				parentElement = parentElement.parentElement;
			} while (parentElement);
		}

		// Always update the scroll top tracker if possible.
		if (scrollParent) {
			scrollParent.fastClickLastScrollTop = scrollParent.scrollTop;
		}
	};


	/**
	 * @param {EventTarget} targetElement
	 * @returns {Element|EventTarget}
	 */
	FastClick.prototype.getTargetElementFromEventTarget = function(eventTarget) {

		// On some older browsers (notably Safari on iOS 4.1 - see issue #56) the event target may be a text node.
		if (eventTarget.nodeType === Node.TEXT_NODE) {
			return eventTarget.parentNode;
		}

		return eventTarget;
	};


	/**
	 * On touch start, record the position and scroll offset.
	 *
	 * @param {Event} event
	 * @returns {boolean}
	 */
	FastClick.prototype.onTouchStart = function(event) {
		var targetElement, touch, selection;

		// Ignore multiple touches, otherwise pinch-to-zoom is prevented if both fingers are on the FastClick element (issue #111).
		if (event.targetTouches.length > 1) {
			return true;
		}

		targetElement = this.getTargetElementFromEventTarget(event.target);
		touch = event.targetTouches[0];

		if (deviceIsIOS) {

			// Only trusted events will deselect text on iOS (issue #49)
			selection = window.getSelection();
			if (selection.rangeCount && !selection.isCollapsed) {
				return true;
			}

			if (!deviceIsIOS4) {

				// Weird things happen on iOS when an alert or confirm dialog is opened from a click event callback (issue #23):
				// when the user next taps anywhere else on the page, new touchstart and touchend events are dispatched
				// with the same identifier as the touch event that previously triggered the click that triggered the alert.
				// Sadly, there is an issue on iOS 4 that causes some normal touch events to have the same identifier as an
				// immediately preceeding touch event (issue #52), so this fix is unavailable on that platform.
				// Issue 120: touch.identifier is 0 when Chrome dev tools 'Emulate touch events' is set with an iOS device UA string,
				// which causes all touch events to be ignored. As this block only applies to iOS, and iOS identifiers are always long,
				// random integers, it's safe to to continue if the identifier is 0 here.
				if (touch.identifier && touch.identifier === this.lastTouchIdentifier) {
					event.preventDefault();
					return false;
				}

				this.lastTouchIdentifier = touch.identifier;

				// If the target element is a child of a scrollable layer (using -webkit-overflow-scrolling: touch) and:
				// 1) the user does a fling scroll on the scrollable layer
				// 2) the user stops the fling scroll with another tap
				// then the event.target of the last 'touchend' event will be the element that was under the user's finger
				// when the fling scroll was started, causing FastClick to send a click event to that layer - unless a check
				// is made to ensure that a parent layer was not scrolled before sending a synthetic click (issue #42).
				this.updateScrollParent(targetElement);
			}
		}

		this.trackingClick = true;
		this.trackingClickStart = event.timeStamp;
		this.targetElement = targetElement;

		this.touchStartX = touch.pageX;
		this.touchStartY = touch.pageY;

		// Prevent phantom clicks on fast double-tap (issue #36)
		if ((event.timeStamp - this.lastClickTime) < this.tapDelay) {
			event.preventDefault();
		}

		return true;
	};


	/**
	 * Based on a touchmove event object, check whether the touch has moved past a boundary since it started.
	 *
	 * @param {Event} event
	 * @returns {boolean}
	 */
	FastClick.prototype.touchHasMoved = function(event) {
		var touch = event.changedTouches[0], boundary = this.touchBoundary;

		if (Math.abs(touch.pageX - this.touchStartX) > boundary || Math.abs(touch.pageY - this.touchStartY) > boundary) {
			return true;
		}

		return false;
	};


	/**
	 * Update the last position.
	 *
	 * @param {Event} event
	 * @returns {boolean}
	 */
	FastClick.prototype.onTouchMove = function(event) {
		if (!this.trackingClick) {
			return true;
		}

		// If the touch has moved, cancel the click tracking
		if (this.targetElement !== this.getTargetElementFromEventTarget(event.target) || this.touchHasMoved(event)) {
			this.trackingClick = false;
			this.targetElement = null;
		}

		return true;
	};


	/**
	 * Attempt to find the labelled control for the given label element.
	 *
	 * @param {EventTarget|HTMLLabelElement} labelElement
	 * @returns {Element|null}
	 */
	FastClick.prototype.findControl = function(labelElement) {

		// Fast path for newer browsers supporting the HTML5 control attribute
		if (labelElement.control !== undefined) {
			return labelElement.control;
		}

		// All browsers under test that support touch events also support the HTML5 htmlFor attribute
		if (labelElement.htmlFor) {
			return document.getElementById(labelElement.htmlFor);
		}

		// If no for attribute exists, attempt to retrieve the first labellable descendant element
		// the list of which is defined here: http://www.w3.org/TR/html5/forms.html#category-label
		return labelElement.querySelector('button, input:not([type=hidden]), keygen, meter, output, progress, select, textarea');
	};


	/**
	 * On touch end, determine whether to send a click event at once.
	 *
	 * @param {Event} event
	 * @returns {boolean}
	 */
	FastClick.prototype.onTouchEnd = function(event) {
		var forElement, trackingClickStart, targetTagName, scrollParent, touch, targetElement = this.targetElement;

		if (!this.trackingClick) {
			return true;
		}

		// Prevent phantom clicks on fast double-tap (issue #36)
		if ((event.timeStamp - this.lastClickTime) < this.tapDelay) {
			this.cancelNextClick = true;
			return true;
		}

		if ((event.timeStamp - this.trackingClickStart) > this.tapTimeout) {
			return true;
		}

		// Reset to prevent wrong click cancel on input (issue #156).
		this.cancelNextClick = false;

		this.lastClickTime = event.timeStamp;

		trackingClickStart = this.trackingClickStart;
		this.trackingClick = false;
		this.trackingClickStart = 0;

		// On some iOS devices, the targetElement supplied with the event is invalid if the layer
		// is performing a transition or scroll, and has to be re-detected manually. Note that
		// for this to function correctly, it must be called *after* the event target is checked!
		// See issue #57; also filed as rdar://13048589 .
		if (deviceIsIOSWithBadTarget) {
			touch = event.changedTouches[0];

			// In certain cases arguments of elementFromPoint can be negative, so prevent setting targetElement to null
			targetElement = document.elementFromPoint(touch.pageX - window.pageXOffset, touch.pageY - window.pageYOffset) || targetElement;
			targetElement.fastClickScrollParent = this.targetElement.fastClickScrollParent;
		}

		targetTagName = targetElement.tagName.toLowerCase();
		if (targetTagName === 'label') {
			forElement = this.findControl(targetElement);
			if (forElement) {
				this.focus(targetElement);
				if (deviceIsAndroid) {
					return false;
				}

				targetElement = forElement;
			}
		} else if (this.needsFocus(targetElement)) {

			// Case 1: If the touch started a while ago (best guess is 100ms based on tests for issue #36) then focus will be triggered anyway. Return early and unset the target element reference so that the subsequent click will be allowed through.
			// Case 2: Without this exception for input elements tapped when the document is contained in an iframe, then any inputted text won't be visible even though the value attribute is updated as the user types (issue #37).
			if ((event.timeStamp - trackingClickStart) > 100 || (deviceIsIOS && window.top !== window && targetTagName === 'input')) {
				this.targetElement = null;
				return false;
			}

			this.focus(targetElement);
			this.sendClick(targetElement, event);

			// Select elements need the event to go through on iOS 4, otherwise the selector menu won't open.
			// Also this breaks opening selects when VoiceOver is active on iOS6, iOS7 (and possibly others)
			if (!deviceIsIOS || targetTagName !== 'select') {
				this.targetElement = null;
				event.preventDefault();
			}

			return false;
		}

		if (deviceIsIOS && !deviceIsIOS4) {

			// Don't send a synthetic click event if the target element is contained within a parent layer that was scrolled
			// and this tap is being used to stop the scrolling (usually initiated by a fling - issue #42).
			scrollParent = targetElement.fastClickScrollParent;
			if (scrollParent && scrollParent.fastClickLastScrollTop !== scrollParent.scrollTop) {
				return true;
			}
		}

		// Prevent the actual click from going though - unless the target node is marked as requiring
		// real clicks or if it is in the whitelist in which case only non-programmatic clicks are permitted.
		if (!this.needsClick(targetElement)) {
			event.preventDefault();
			this.sendClick(targetElement, event);
		}

		return false;
	};


	/**
	 * On touch cancel, stop tracking the click.
	 *
	 * @returns {void}
	 */
	FastClick.prototype.onTouchCancel = function() {
		this.trackingClick = false;
		this.targetElement = null;
	};


	/**
	 * Determine mouse events which should be permitted.
	 *
	 * @param {Event} event
	 * @returns {boolean}
	 */
	FastClick.prototype.onMouse = function(event) {

		// If a target element was never set (because a touch event was never fired) allow the event
		if (!this.targetElement) {
			return true;
		}

		if (event.forwardedTouchEvent) {
			return true;
		}

		// Programmatically generated events targeting a specific element should be permitted
		if (!event.cancelable) {
			return true;
		}

		// Derive and check the target element to see whether the mouse event needs to be permitted;
		// unless explicitly enabled, prevent non-touch click events from triggering actions,
		// to prevent ghost/doubleclicks.
		if (!this.needsClick(this.targetElement) || this.cancelNextClick) {

			// Prevent any user-added listeners declared on FastClick element from being fired.
			if (event.stopImmediatePropagation) {
				event.stopImmediatePropagation();
			} else {

				// Part of the hack for browsers that don't support Event#stopImmediatePropagation (e.g. Android 2)
				event.propagationStopped = true;
			}

			// Cancel the event
			event.stopPropagation();
			event.preventDefault();

			return false;
		}

		// If the mouse event is permitted, return true for the action to go through.
		return true;
	};


	/**
	 * On actual clicks, determine whether this is a touch-generated click, a click action occurring
	 * naturally after a delay after a touch (which needs to be cancelled to avoid duplication), or
	 * an actual click which should be permitted.
	 *
	 * @param {Event} event
	 * @returns {boolean}
	 */
	FastClick.prototype.onClick = function(event) {
		var permitted;

		// It's possible for another FastClick-like library delivered with third-party code to fire a click event before FastClick does (issue #44). In that case, set the click-tracking flag back to false and return early. This will cause onTouchEnd to return early.
		if (this.trackingClick) {
			this.targetElement = null;
			this.trackingClick = false;
			return true;
		}

		// Very odd behaviour on iOS (issue #18): if a submit element is present inside a form and the user hits enter in the iOS simulator or clicks the Go button on the pop-up OS keyboard the a kind of 'fake' click event will be triggered with the submit-type input element as the target.
		if (event.target.type === 'submit' && event.detail === 0) {
			return true;
		}

		permitted = this.onMouse(event);

		// Only unset targetElement if the click is not permitted. This will ensure that the check for !targetElement in onMouse fails and the browser's click doesn't go through.
		if (!permitted) {
			this.targetElement = null;
		}

		// If clicks are permitted, return true for the action to go through.
		return permitted;
	};


	/**
	 * Remove all FastClick's event listeners.
	 *
	 * @returns {void}
	 */
	FastClick.prototype.destroy = function() {
		var layer = this.layer;

		if (deviceIsAndroid) {
			layer.removeEventListener('mouseover', this.onMouse, true);
			layer.removeEventListener('mousedown', this.onMouse, true);
			layer.removeEventListener('mouseup', this.onMouse, true);
		}

		layer.removeEventListener('click', this.onClick, true);
		layer.removeEventListener('touchstart', this.onTouchStart, false);
		layer.removeEventListener('touchmove', this.onTouchMove, false);
		layer.removeEventListener('touchend', this.onTouchEnd, false);
		layer.removeEventListener('touchcancel', this.onTouchCancel, false);
	};


	/**
	 * Check whether FastClick is needed.
	 *
	 * @param {Element} layer The layer to listen on
	 */
	FastClick.notNeeded = function(layer) {
		var metaViewport;
		var chromeVersion;
		var blackberryVersion;
		var firefoxVersion;

		// Devices that don't support touch don't need FastClick
		if (typeof window.ontouchstart === 'undefined') {
			return true;
		}

		// Chrome version - zero for other browsers
		chromeVersion = +(/Chrome\/([0-9]+)/.exec(navigator.userAgent) || [,0])[1];

		if (chromeVersion) {

			if (deviceIsAndroid) {
				metaViewport = document.querySelector('meta[name=viewport]');

				if (metaViewport) {
					// Chrome on Android with user-scalable="no" doesn't need FastClick (issue #89)
					if (metaViewport.content.indexOf('user-scalable=no') !== -1) {
						return true;
					}
					// Chrome 32 and above with width=device-width or less don't need FastClick
					if (chromeVersion > 31 && document.documentElement.scrollWidth <= window.outerWidth) {
						return true;
					}
				}

			// Chrome desktop doesn't need FastClick (issue #15)
			} else {
				return true;
			}
		}

		if (deviceIsBlackBerry10) {
			blackberryVersion = navigator.userAgent.match(/Version\/([0-9]*)\.([0-9]*)/);

			// BlackBerry 10.3+ does not require Fastclick library.
			// https://github.com/ftlabs/fastclick/issues/251
			if (blackberryVersion[1] >= 10 && blackberryVersion[2] >= 3) {
				metaViewport = document.querySelector('meta[name=viewport]');

				if (metaViewport) {
					// user-scalable=no eliminates click delay.
					if (metaViewport.content.indexOf('user-scalable=no') !== -1) {
						return true;
					}
					// width=device-width (or less than device-width) eliminates click delay.
					if (document.documentElement.scrollWidth <= window.outerWidth) {
						return true;
					}
				}
			}
		}

		// IE10 with -ms-touch-action: none or manipulation, which disables double-tap-to-zoom (issue #97)
		if (layer.style.msTouchAction === 'none' || layer.style.touchAction === 'manipulation') {
			return true;
		}

		// Firefox version - zero for other browsers
		firefoxVersion = +(/Firefox\/([0-9]+)/.exec(navigator.userAgent) || [,0])[1];

		if (firefoxVersion >= 27) {
			// Firefox 27+ does not have tap delay if the content is not zoomable - https://bugzilla.mozilla.org/show_bug.cgi?id=922896

			metaViewport = document.querySelector('meta[name=viewport]');
			if (metaViewport && (metaViewport.content.indexOf('user-scalable=no') !== -1 || document.documentElement.scrollWidth <= window.outerWidth)) {
				return true;
			}
		}

		// IE11: prefixed -ms-touch-action is no longer supported and it's recomended to use non-prefixed version
		// http://msdn.microsoft.com/en-us/library/windows/apps/Hh767313.aspx
		if (layer.style.touchAction === 'none' || layer.style.touchAction === 'manipulation') {
			return true;
		}

		return false;
	};


	/**
	 * Factory method for creating a FastClick object
	 *
	 * @param {Element} layer The layer to listen on
	 * @param {Object} [options={}] The options to override the defaults
	 */
	FastClick.attach = function(layer, options) {
		return new FastClick(layer, options);
	};


	if (typeof define === 'function' && typeof define.amd === 'object' && define.amd) {

		// AMD. Register as an anonymous module.
		define('fastclick',[],function() {
			return FastClick;
		});
	} else if (typeof module !== 'undefined' && module.exports) {
		module.exports = FastClick.attach;
		module.exports.FastClick = FastClick;
	} else {
		window.FastClick = FastClick;
	}
	setTimeout(function(){
		FastClick.attach(document.body);
	}	, 1000);
}());

//     Underscore.js 1.8.3
//     http://underscorejs.org
//     (c) 2009-2015 Jeremy Ashkenas, DocumentCloud and Investigative Reporters & Editors
//     Underscore may be freely distributed under the MIT license.

(function() {

  // Baseline setup
  // --------------

  // Establish the root object, `window` in the browser, or `exports` on the server.
  var root = this;

  // Save the previous value of the `_` variable.
  var previousUnderscore = root._;

  // Save bytes in the minified (but not gzipped) version:
  var ArrayProto = Array.prototype, ObjProto = Object.prototype, FuncProto = Function.prototype;

  // Create quick reference variables for speed access to core prototypes.
  var
    push             = ArrayProto.push,
    slice            = ArrayProto.slice,
    toString         = ObjProto.toString,
    hasOwnProperty   = ObjProto.hasOwnProperty;

  // All **ECMAScript 5** native function implementations that we hope to use
  // are declared here.
  var
    nativeIsArray      = Array.isArray,
    nativeKeys         = Object.keys,
    nativeBind         = FuncProto.bind,
    nativeCreate       = Object.create;

  // Naked function reference for surrogate-prototype-swapping.
  var Ctor = function(){};

  // Create a safe reference to the Underscore object for use below.
  var _ = function(obj) {
    if (obj instanceof _) return obj;
    if (!(this instanceof _)) return new _(obj);
    this._wrapped = obj;
  };

  // Export the Underscore object for **Node.js**, with
  // backwards-compatibility for the old `require()` API. If we're in
  // the browser, add `_` as a global object.
  if (typeof exports !== 'undefined') {
    if (typeof module !== 'undefined' && module.exports) {
      exports = module.exports = _;
    }
    exports._ = _;
  } else {
    root._ = _;
  }

  // Current version.
  _.VERSION = '1.8.3';

  // Internal function that returns an efficient (for current engines) version
  // of the passed-in callback, to be repeatedly applied in other Underscore
  // functions.
  var optimizeCb = function(func, context, argCount) {
    if (context === void 0) return func;
    switch (argCount == null ? 3 : argCount) {
      case 1: return function(value) {
        return func.call(context, value);
      };
      case 2: return function(value, other) {
        return func.call(context, value, other);
      };
      case 3: return function(value, index, collection) {
        return func.call(context, value, index, collection);
      };
      case 4: return function(accumulator, value, index, collection) {
        return func.call(context, accumulator, value, index, collection);
      };
    }
    return function() {
      return func.apply(context, arguments);
    };
  };

  // A mostly-internal function to generate callbacks that can be applied
  // to each element in a collection, returning the desired result \u2014 either
  // identity, an arbitrary callback, a property matcher, or a property accessor.
  var cb = function(value, context, argCount) {
    if (value == null) return _.identity;
    if (_.isFunction(value)) return optimizeCb(value, context, argCount);
    if (_.isObject(value)) return _.matcher(value);
    return _.property(value);
  };
  _.iteratee = function(value, context) {
    return cb(value, context, Infinity);
  };

  // An internal function for creating assigner functions.
  var createAssigner = function(keysFunc, undefinedOnly) {
    return function(obj) {
      var length = arguments.length;
      if (length < 2 || obj == null) return obj;
      for (var index = 1; index < length; index++) {
        var source = arguments[index],
            keys = keysFunc(source),
            l = keys.length;
        for (var i = 0; i < l; i++) {
          var key = keys[i];
          if (!undefinedOnly || obj[key] === void 0) obj[key] = source[key];
        }
      }
      return obj;
    };
  };

  // An internal function for creating a new object that inherits from another.
  var baseCreate = function(prototype) {
    if (!_.isObject(prototype)) return {};
    if (nativeCreate) return nativeCreate(prototype);
    Ctor.prototype = prototype;
    var result = new Ctor;
    Ctor.prototype = null;
    return result;
  };

  var property = function(key) {
    return function(obj) {
      return obj == null ? void 0 : obj[key];
    };
  };

  // Helper for collection methods to determine whether a collection
  // should be iterated as an array or as an object
  // Related: http://people.mozilla.org/~jorendorff/es6-draft.html#sec-tolength
  // Avoids a very nasty iOS 8 JIT bug on ARM-64. #2094
  var MAX_ARRAY_INDEX = Math.pow(2, 53) - 1;
  var getLength = property('length');
  var isArrayLike = function(collection) {
    var length = getLength(collection);
    return typeof length == 'number' && length >= 0 && length <= MAX_ARRAY_INDEX;
  };

  // Collection Functions
  // --------------------

  // The cornerstone, an `each` implementation, aka `forEach`.
  // Handles raw objects in addition to array-likes. Treats all
  // sparse array-likes as if they were dense.
  _.each = _.forEach = function(obj, iteratee, context) {
    iteratee = optimizeCb(iteratee, context);
    var i, length;
    if (isArrayLike(obj)) {
      for (i = 0, length = obj.length; i < length; i++) {
        iteratee(obj[i], i, obj);
      }
    } else {
      var keys = _.keys(obj);
      for (i = 0, length = keys.length; i < length; i++) {
        iteratee(obj[keys[i]], keys[i], obj);
      }
    }
    return obj;
  };

  // Return the results of applying the iteratee to each element.
  _.map = _.collect = function(obj, iteratee, context) {
    iteratee = cb(iteratee, context);
    var keys = !isArrayLike(obj) && _.keys(obj),
        length = (keys || obj).length,
        results = Array(length);
    for (var index = 0; index < length; index++) {
      var currentKey = keys ? keys[index] : index;
      results[index] = iteratee(obj[currentKey], currentKey, obj);
    }
    return results;
  };

  // Create a reducing function iterating left or right.
  function createReduce(dir) {
    // Optimized iterator function as using arguments.length
    // in the main function will deoptimize the, see #1991.
    function iterator(obj, iteratee, memo, keys, index, length) {
      for (; index >= 0 && index < length; index += dir) {
        var currentKey = keys ? keys[index] : index;
        memo = iteratee(memo, obj[currentKey], currentKey, obj);
      }
      return memo;
    }

    return function(obj, iteratee, memo, context) {
      iteratee = optimizeCb(iteratee, context, 4);
      var keys = !isArrayLike(obj) && _.keys(obj),
          length = (keys || obj).length,
          index = dir > 0 ? 0 : length - 1;
      // Determine the initial value if none is provided.
      if (arguments.length < 3) {
        memo = obj[keys ? keys[index] : index];
        index += dir;
      }
      return iterator(obj, iteratee, memo, keys, index, length);
    };
  }

  // **Reduce** builds up a single result from a list of values, aka `inject`,
  // or `foldl`.
  _.reduce = _.foldl = _.inject = createReduce(1);

  // The right-associative version of reduce, also known as `foldr`.
  _.reduceRight = _.foldr = createReduce(-1);

  // Return the first value which passes a truth test. Aliased as `detect`.
  _.find = _.detect = function(obj, predicate, context) {
    var key;
    if (isArrayLike(obj)) {
      key = _.findIndex(obj, predicate, context);
    } else {
      key = _.findKey(obj, predicate, context);
    }
    if (key !== void 0 && key !== -1) return obj[key];
  };

  // Return all the elements that pass a truth test.
  // Aliased as `select`.
  _.filter = _.select = function(obj, predicate, context) {
    var results = [];
    predicate = cb(predicate, context);
    _.each(obj, function(value, index, list) {
      if (predicate(value, index, list)) results.push(value);
    });
    return results;
  };

  // Return all the elements for which a truth test fails.
  _.reject = function(obj, predicate, context) {
    return _.filter(obj, _.negate(cb(predicate)), context);
  };

  // Determine whether all of the elements match a truth test.
  // Aliased as `all`.
  _.every = _.all = function(obj, predicate, context) {
    predicate = cb(predicate, context);
    var keys = !isArrayLike(obj) && _.keys(obj),
        length = (keys || obj).length;
    for (var index = 0; index < length; index++) {
      var currentKey = keys ? keys[index] : index;
      if (!predicate(obj[currentKey], currentKey, obj)) return false;
    }
    return true;
  };

  // Determine if at least one element in the object matches a truth test.
  // Aliased as `any`.
  _.some = _.any = function(obj, predicate, context) {
    predicate = cb(predicate, context);
    var keys = !isArrayLike(obj) && _.keys(obj),
        length = (keys || obj).length;
    for (var index = 0; index < length; index++) {
      var currentKey = keys ? keys[index] : index;
      if (predicate(obj[currentKey], currentKey, obj)) return true;
    }
    return false;
  };

  // Determine if the array or object contains a given item (using `===`).
  // Aliased as `includes` and `include`.
  _.contains = _.includes = _.include = function(obj, item, fromIndex, guard) {
    if (!isArrayLike(obj)) obj = _.values(obj);
    if (typeof fromIndex != 'number' || guard) fromIndex = 0;
    return _.indexOf(obj, item, fromIndex) >= 0;
  };

  // Invoke a method (with arguments) on every item in a collection.
  _.invoke = function(obj, method) {
    var args = slice.call(arguments, 2);
    var isFunc = _.isFunction(method);
    return _.map(obj, function(value) {
      var func = isFunc ? method : value[method];
      return func == null ? func : func.apply(value, args);
    });
  };

  // Convenience version of a common use case of `map`: fetching a property.
  _.pluck = function(obj, key) {
    return _.map(obj, _.property(key));
  };

  // Convenience version of a common use case of `filter`: selecting only objects
  // containing specific `key:value` pairs.
  _.where = function(obj, attrs) {
    return _.filter(obj, _.matcher(attrs));
  };

  // Convenience version of a common use case of `find`: getting the first object
  // containing specific `key:value` pairs.
  _.findWhere = function(obj, attrs) {
    return _.find(obj, _.matcher(attrs));
  };

  // Return the maximum element (or element-based computation).
  _.max = function(obj, iteratee, context) {
    var result = -Infinity, lastComputed = -Infinity,
        value, computed;
    if (iteratee == null && obj != null) {
      obj = isArrayLike(obj) ? obj : _.values(obj);
      for (var i = 0, length = obj.length; i < length; i++) {
        value = obj[i];
        if (value > result) {
          result = value;
        }
      }
    } else {
      iteratee = cb(iteratee, context);
      _.each(obj, function(value, index, list) {
        computed = iteratee(value, index, list);
        if (computed > lastComputed || computed === -Infinity && result === -Infinity) {
          result = value;
          lastComputed = computed;
        }
      });
    }
    return result;
  };

  // Return the minimum element (or element-based computation).
  _.min = function(obj, iteratee, context) {
    var result = Infinity, lastComputed = Infinity,
        value, computed;
    if (iteratee == null && obj != null) {
      obj = isArrayLike(obj) ? obj : _.values(obj);
      for (var i = 0, length = obj.length; i < length; i++) {
        value = obj[i];
        if (value < result) {
          result = value;
        }
      }
    } else {
      iteratee = cb(iteratee, context);
      _.each(obj, function(value, index, list) {
        computed = iteratee(value, index, list);
        if (computed < lastComputed || computed === Infinity && result === Infinity) {
          result = value;
          lastComputed = computed;
        }
      });
    }
    return result;
  };

  // Shuffle a collection, using the modern version of the
  // [Fisher-Yates shuffle](http://en.wikipedia.org/wiki/Fisher\u2013Yates_shuffle).
  _.shuffle = function(obj) {
    var set = isArrayLike(obj) ? obj : _.values(obj);
    var length = set.length;
    var shuffled = Array(length);
    for (var index = 0, rand; index < length; index++) {
      rand = _.random(0, index);
      if (rand !== index) shuffled[index] = shuffled[rand];
      shuffled[rand] = set[index];
    }
    return shuffled;
  };

  // Sample **n** random values from a collection.
  // If **n** is not specified, returns a single random element.
  // The internal `guard` argument allows it to work with `map`.
  _.sample = function(obj, n, guard) {
    if (n == null || guard) {
      if (!isArrayLike(obj)) obj = _.values(obj);
      return obj[_.random(obj.length - 1)];
    }
    return _.shuffle(obj).slice(0, Math.max(0, n));
  };

  // Sort the object's values by a criterion produced by an iteratee.
  _.sortBy = function(obj, iteratee, context) {
    iteratee = cb(iteratee, context);
    return _.pluck(_.map(obj, function(value, index, list) {
      return {
        value: value,
        index: index,
        criteria: iteratee(value, index, list)
      };
    }).sort(function(left, right) {
      var a = left.criteria;
      var b = right.criteria;
      if (a !== b) {
        if (a > b || a === void 0) return 1;
        if (a < b || b === void 0) return -1;
      }
      return left.index - right.index;
    }), 'value');
  };

  // An internal function used for aggregate "group by" operations.
  var group = function(behavior) {
    return function(obj, iteratee, context) {
      var result = {};
      iteratee = cb(iteratee, context);
      _.each(obj, function(value, index) {
        var key = iteratee(value, index, obj);
        behavior(result, value, key);
      });
      return result;
    };
  };

  // Groups the object's values by a criterion. Pass either a string attribute
  // to group by, or a function that returns the criterion.
  _.groupBy = group(function(result, value, key) {
    if (_.has(result, key)) result[key].push(value); else result[key] = [value];
  });

  // Indexes the object's values by a criterion, similar to `groupBy`, but for
  // when you know that your index values will be unique.
  _.indexBy = group(function(result, value, key) {
    result[key] = value;
  });

  // Counts instances of an object that group by a certain criterion. Pass
  // either a string attribute to count by, or a function that returns the
  // criterion.
  _.countBy = group(function(result, value, key) {
    if (_.has(result, key)) result[key]++; else result[key] = 1;
  });

  // Safely create a real, live array from anything iterable.
  _.toArray = function(obj) {
    if (!obj) return [];
    if (_.isArray(obj)) return slice.call(obj);
    if (isArrayLike(obj)) return _.map(obj, _.identity);
    return _.values(obj);
  };

  // Return the number of elements in an object.
  _.size = function(obj) {
    if (obj == null) return 0;
    return isArrayLike(obj) ? obj.length : _.keys(obj).length;
  };

  // Split a collection into two arrays: one whose elements all satisfy the given
  // predicate, and one whose elements all do not satisfy the predicate.
  _.partition = function(obj, predicate, context) {
    predicate = cb(predicate, context);
    var pass = [], fail = [];
    _.each(obj, function(value, key, obj) {
      (predicate(value, key, obj) ? pass : fail).push(value);
    });
    return [pass, fail];
  };

  // Array Functions
  // ---------------

  // Get the first element of an array. Passing **n** will return the first N
  // values in the array. Aliased as `head` and `take`. The **guard** check
  // allows it to work with `_.map`.
  _.first = _.head = _.take = function(array, n, guard) {
    if (array == null) return void 0;
    if (n == null || guard) return array[0];
    return _.initial(array, array.length - n);
  };

  // Returns everything but the last entry of the array. Especially useful on
  // the arguments object. Passing **n** will return all the values in
  // the array, excluding the last N.
  _.initial = function(array, n, guard) {
    return slice.call(array, 0, Math.max(0, array.length - (n == null || guard ? 1 : n)));
  };

  // Get the last element of an array. Passing **n** will return the last N
  // values in the array.
  _.last = function(array, n, guard) {
    if (array == null) return void 0;
    if (n == null || guard) return array[array.length - 1];
    return _.rest(array, Math.max(0, array.length - n));
  };

  // Returns everything but the first entry of the array. Aliased as `tail` and `drop`.
  // Especially useful on the arguments object. Passing an **n** will return
  // the rest N values in the array.
  _.rest = _.tail = _.drop = function(array, n, guard) {
    return slice.call(array, n == null || guard ? 1 : n);
  };

  // Trim out all falsy values from an array.
  _.compact = function(array) {
    return _.filter(array, _.identity);
  };

  // Internal implementation of a recursive `flatten` function.
  var flatten = function(input, shallow, strict, startIndex) {
    var output = [], idx = 0;
    for (var i = startIndex || 0, length = getLength(input); i < length; i++) {
      var value = input[i];
      if (isArrayLike(value) && (_.isArray(value) || _.isArguments(value))) {
        //flatten current level of array or arguments object
        if (!shallow) value = flatten(value, shallow, strict);
        var j = 0, len = value.length;
        output.length += len;
        while (j < len) {
          output[idx++] = value[j++];
        }
      } else if (!strict) {
        output[idx++] = value;
      }
    }
    return output;
  };

  // Flatten out an array, either recursively (by default), or just one level.
  _.flatten = function(array, shallow) {
    return flatten(array, shallow, false);
  };

  // Return a version of the array that does not contain the specified value(s).
  _.without = function(array) {
    return _.difference(array, slice.call(arguments, 1));
  };

  // Produce a duplicate-free version of the array. If the array has already
  // been sorted, you have the option of using a faster algorithm.
  // Aliased as `unique`.
  _.uniq = _.unique = function(array, isSorted, iteratee, context) {
    if (!_.isBoolean(isSorted)) {
      context = iteratee;
      iteratee = isSorted;
      isSorted = false;
    }
    if (iteratee != null) iteratee = cb(iteratee, context);
    var result = [];
    var seen = [];
    for (var i = 0, length = getLength(array); i < length; i++) {
      var value = array[i],
          computed = iteratee ? iteratee(value, i, array) : value;
      if (isSorted) {
        if (!i || seen !== computed) result.push(value);
        seen = computed;
      } else if (iteratee) {
        if (!_.contains(seen, computed)) {
          seen.push(computed);
          result.push(value);
        }
      } else if (!_.contains(result, value)) {
        result.push(value);
      }
    }
    return result;
  };

  // Produce an array that contains the union: each distinct element from all of
  // the passed-in arrays.
  _.union = function() {
    return _.uniq(flatten(arguments, true, true));
  };

  // Produce an array that contains every item shared between all the
  // passed-in arrays.
  _.intersection = function(array) {
    var result = [];
    var argsLength = arguments.length;
    for (var i = 0, length = getLength(array); i < length; i++) {
      var item = array[i];
      if (_.contains(result, item)) continue;
      for (var j = 1; j < argsLength; j++) {
        if (!_.contains(arguments[j], item)) break;
      }
      if (j === argsLength) result.push(item);
    }
    return result;
  };

  // Take the difference between one array and a number of other arrays.
  // Only the elements present in just the first array will remain.
  _.difference = function(array) {
    var rest = flatten(arguments, true, true, 1);
    return _.filter(array, function(value){
      return !_.contains(rest, value);
    });
  };

  // Zip together multiple lists into a single array -- elements that share
  // an index go together.
  _.zip = function() {
    return _.unzip(arguments);
  };

  // Complement of _.zip. Unzip accepts an array of arrays and groups
  // each array's elements on shared indices
  _.unzip = function(array) {
    var length = array && _.max(array, getLength).length || 0;
    var result = Array(length);

    for (var index = 0; index < length; index++) {
      result[index] = _.pluck(array, index);
    }
    return result;
  };

  // Converts lists into objects. Pass either a single array of `[key, value]`
  // pairs, or two parallel arrays of the same length -- one of keys, and one of
  // the corresponding values.
  _.object = function(list, values) {
    var result = {};
    for (var i = 0, length = getLength(list); i < length; i++) {
      if (values) {
        result[list[i]] = values[i];
      } else {
        result[list[i][0]] = list[i][1];
      }
    }
    return result;
  };

  // Generator function to create the findIndex and findLastIndex functions
  function createPredicateIndexFinder(dir) {
    return function(array, predicate, context) {
      predicate = cb(predicate, context);
      var length = getLength(array);
      var index = dir > 0 ? 0 : length - 1;
      for (; index >= 0 && index < length; index += dir) {
        if (predicate(array[index], index, array)) return index;
      }
      return -1;
    };
  }

  // Returns the first index on an array-like that passes a predicate test
  _.findIndex = createPredicateIndexFinder(1);
  _.findLastIndex = createPredicateIndexFinder(-1);

  // Use a comparator function to figure out the smallest index at which
  // an object should be inserted so as to maintain order. Uses binary search.
  _.sortedIndex = function(array, obj, iteratee, context) {
    iteratee = cb(iteratee, context, 1);
    var value = iteratee(obj);
    var low = 0, high = getLength(array);
    while (low < high) {
      var mid = Math.floor((low + high) / 2);
      if (iteratee(array[mid]) < value) low = mid + 1; else high = mid;
    }
    return low;
  };

  // Generator function to create the indexOf and lastIndexOf functions
  function createIndexFinder(dir, predicateFind, sortedIndex) {
    return function(array, item, idx) {
      var i = 0, length = getLength(array);
      if (typeof idx == 'number') {
        if (dir > 0) {
            i = idx >= 0 ? idx : Math.max(idx + length, i);
        } else {
            length = idx >= 0 ? Math.min(idx + 1, length) : idx + length + 1;
        }
      } else if (sortedIndex && idx && length) {
        idx = sortedIndex(array, item);
        return array[idx] === item ? idx : -1;
      }
      if (item !== item) {
        idx = predicateFind(slice.call(array, i, length), _.isNaN);
        return idx >= 0 ? idx + i : -1;
      }
      for (idx = dir > 0 ? i : length - 1; idx >= 0 && idx < length; idx += dir) {
        if (array[idx] === item) return idx;
      }
      return -1;
    };
  }

  // Return the position of the first occurrence of an item in an array,
  // or -1 if the item is not included in the array.
  // If the array is large and already in sort order, pass `true`
  // for **isSorted** to use binary search.
  _.indexOf = createIndexFinder(1, _.findIndex, _.sortedIndex);
  _.lastIndexOf = createIndexFinder(-1, _.findLastIndex);

  // Generate an integer Array containing an arithmetic progression. A port of
  // the native Python `range()` function. See
  // [the Python documentation](http://docs.python.org/library/functions.html#range).
  _.range = function(start, stop, step) {
    if (stop == null) {
      stop = start || 0;
      start = 0;
    }
    step = step || 1;

    var length = Math.max(Math.ceil((stop - start) / step), 0);
    var range = Array(length);

    for (var idx = 0; idx < length; idx++, start += step) {
      range[idx] = start;
    }

    return range;
  };

  // Function (ahem) Functions
  // ------------------

  // Determines whether to execute a function as a constructor
  // or a normal function with the provided arguments
  var executeBound = function(sourceFunc, boundFunc, context, callingContext, args) {
    if (!(callingContext instanceof boundFunc)) return sourceFunc.apply(context, args);
    var self = baseCreate(sourceFunc.prototype);
    var result = sourceFunc.apply(self, args);
    if (_.isObject(result)) return result;
    return self;
  };

  // Create a function bound to a given object (assigning `this`, and arguments,
  // optionally). Delegates to **ECMAScript 5**'s native `Function.bind` if
  // available.
  _.bind = function(func, context) {
    if (nativeBind && func.bind === nativeBind) return nativeBind.apply(func, slice.call(arguments, 1));
    if (!_.isFunction(func)) throw new TypeError('Bind must be called on a function');
    var args = slice.call(arguments, 2);
    var bound = function() {
      return executeBound(func, bound, context, this, args.concat(slice.call(arguments)));
    };
    return bound;
  };

  // Partially apply a function by creating a version that has had some of its
  // arguments pre-filled, without changing its dynamic `this` context. _ acts
  // as a placeholder, allowing any combination of arguments to be pre-filled.
  _.partial = function(func) {
    var boundArgs = slice.call(arguments, 1);
    var bound = function() {
      var position = 0, length = boundArgs.length;
      var args = Array(length);
      for (var i = 0; i < length; i++) {
        args[i] = boundArgs[i] === _ ? arguments[position++] : boundArgs[i];
      }
      while (position < arguments.length) args.push(arguments[position++]);
      return executeBound(func, bound, this, this, args);
    };
    return bound;
  };

  // Bind a number of an object's methods to that object. Remaining arguments
  // are the method names to be bound. Useful for ensuring that all callbacks
  // defined on an object belong to it.
  _.bindAll = function(obj) {
    var i, length = arguments.length, key;
    if (length <= 1) throw new Error('bindAll must be passed function names');
    for (i = 1; i < length; i++) {
      key = arguments[i];
      obj[key] = _.bind(obj[key], obj);
    }
    return obj;
  };

  // Memoize an expensive function by storing its results.
  _.memoize = function(func, hasher) {
    var memoize = function(key) {
      var cache = memoize.cache;
      var address = '' + (hasher ? hasher.apply(this, arguments) : key);
      if (!_.has(cache, address)) cache[address] = func.apply(this, arguments);
      return cache[address];
    };
    memoize.cache = {};
    return memoize;
  };

  // Delays a function for the given number of milliseconds, and then calls
  // it with the arguments supplied.
  _.delay = function(func, wait) {
    var args = slice.call(arguments, 2);
    return setTimeout(function(){
      return func.apply(null, args);
    }, wait);
  };

  // Defers a function, scheduling it to run after the current call stack has
  // cleared.
  _.defer = _.partial(_.delay, _, 1);

  // Returns a function, that, when invoked, will only be triggered at most once
  // during a given window of time. Normally, the throttled function will run
  // as much as it can, without ever going more than once per `wait` duration;
  // but if you'd like to disable the execution on the leading edge, pass
  // `{leading: false}`. To disable execution on the trailing edge, ditto.
  _.throttle = function(func, wait, options) {
    var context, args, result;
    var timeout = null;
    var previous = 0;
    if (!options) options = {};
    var later = function() {
      previous = options.leading === false ? 0 : _.now();
      timeout = null;
      result = func.apply(context, args);
      if (!timeout) context = args = null;
    };
    return function() {
      var now = _.now();
      if (!previous && options.leading === false) previous = now;
      var remaining = wait - (now - previous);
      context = this;
      args = arguments;
      if (remaining <= 0 || remaining > wait) {
        if (timeout) {
          clearTimeout(timeout);
          timeout = null;
        }
        previous = now;
        result = func.apply(context, args);
        if (!timeout) context = args = null;
      } else if (!timeout && options.trailing !== false) {
        timeout = setTimeout(later, remaining);
      }
      return result;
    };
  };

  // Returns a function, that, as long as it continues to be invoked, will not
  // be triggered. The function will be called after it stops being called for
  // N milliseconds. If `immediate` is passed, trigger the function on the
  // leading edge, instead of the trailing.
  _.debounce = function(func, wait, immediate) {
    var timeout, args, context, timestamp, result;

    var later = function() {
      var last = _.now() - timestamp;

      if (last < wait && last >= 0) {
        timeout = setTimeout(later, wait - last);
      } else {
        timeout = null;
        if (!immediate) {
          result = func.apply(context, args);
          if (!timeout) context = args = null;
        }
      }
    };

    return function() {
      context = this;
      args = arguments;
      timestamp = _.now();
      var callNow = immediate && !timeout;
      if (!timeout) timeout = setTimeout(later, wait);
      if (callNow) {
        result = func.apply(context, args);
        context = args = null;
      }

      return result;
    };
  };

  // Returns the first function passed as an argument to the second,
  // allowing you to adjust arguments, run code before and after, and
  // conditionally execute the original function.
  _.wrap = function(func, wrapper) {
    return _.partial(wrapper, func);
  };

  // Returns a negated version of the passed-in predicate.
  _.negate = function(predicate) {
    return function() {
      return !predicate.apply(this, arguments);
    };
  };

  // Returns a function that is the composition of a list of functions, each
  // consuming the return value of the function that follows.
  _.compose = function() {
    var args = arguments;
    var start = args.length - 1;
    return function() {
      var i = start;
      var result = args[start].apply(this, arguments);
      while (i--) result = args[i].call(this, result);
      return result;
    };
  };

  // Returns a function that will only be executed on and after the Nth call.
  _.after = function(times, func) {
    return function() {
      if (--times < 1) {
        return func.apply(this, arguments);
      }
    };
  };

  // Returns a function that will only be executed up to (but not including) the Nth call.
  _.before = function(times, func) {
    var memo;
    return function() {
      if (--times > 0) {
        memo = func.apply(this, arguments);
      }
      if (times <= 1) func = null;
      return memo;
    };
  };

  // Returns a function that will be executed at most one time, no matter how
  // often you call it. Useful for lazy initialization.
  _.once = _.partial(_.before, 2);

  // Object Functions
  // ----------------

  // Keys in IE < 9 that won't be iterated by `for key in ...` and thus missed.
  var hasEnumBug = !{toString: null}.propertyIsEnumerable('toString');
  var nonEnumerableProps = ['valueOf', 'isPrototypeOf', 'toString',
                      'propertyIsEnumerable', 'hasOwnProperty', 'toLocaleString'];

  function collectNonEnumProps(obj, keys) {
    var nonEnumIdx = nonEnumerableProps.length;
    var constructor = obj.constructor;
    var proto = (_.isFunction(constructor) && constructor.prototype) || ObjProto;

    // Constructor is a special case.
    var prop = 'constructor';
    if (_.has(obj, prop) && !_.contains(keys, prop)) keys.push(prop);

    while (nonEnumIdx--) {
      prop = nonEnumerableProps[nonEnumIdx];
      if (prop in obj && obj[prop] !== proto[prop] && !_.contains(keys, prop)) {
        keys.push(prop);
      }
    }
  }

  // Retrieve the names of an object's own properties.
  // Delegates to **ECMAScript 5**'s native `Object.keys`
  _.keys = function(obj) {
    if (!_.isObject(obj)) return [];
    if (nativeKeys) return nativeKeys(obj);
    var keys = [];
    for (var key in obj) if (_.has(obj, key)) keys.push(key);
    // Ahem, IE < 9.
    if (hasEnumBug) collectNonEnumProps(obj, keys);
    return keys;
  };

  // Retrieve all the property names of an object.
  _.allKeys = function(obj) {
    if (!_.isObject(obj)) return [];
    var keys = [];
    for (var key in obj) keys.push(key);
    // Ahem, IE < 9.
    if (hasEnumBug) collectNonEnumProps(obj, keys);
    return keys;
  };

  // Retrieve the values of an object's properties.
  _.values = function(obj) {
    var keys = _.keys(obj);
    var length = keys.length;
    var values = Array(length);
    for (var i = 0; i < length; i++) {
      values[i] = obj[keys[i]];
    }
    return values;
  };

  // Returns the results of applying the iteratee to each element of the object
  // In contrast to _.map it returns an object
  _.mapObject = function(obj, iteratee, context) {
    iteratee = cb(iteratee, context);
    var keys =  _.keys(obj),
          length = keys.length,
          results = {},
          currentKey;
      for (var index = 0; index < length; index++) {
        currentKey = keys[index];
        results[currentKey] = iteratee(obj[currentKey], currentKey, obj);
      }
      return results;
  };

  // Convert an object into a list of `[key, value]` pairs.
  _.pairs = function(obj) {
    var keys = _.keys(obj);
    var length = keys.length;
    var pairs = Array(length);
    for (var i = 0; i < length; i++) {
      pairs[i] = [keys[i], obj[keys[i]]];
    }
    return pairs;
  };

  // Invert the keys and values of an object. The values must be serializable.
  _.invert = function(obj) {
    var result = {};
    var keys = _.keys(obj);
    for (var i = 0, length = keys.length; i < length; i++) {
      result[obj[keys[i]]] = keys[i];
    }
    return result;
  };

  // Return a sorted list of the function names available on the object.
  // Aliased as `methods`
  _.functions = _.methods = function(obj) {
    var names = [];
    for (var key in obj) {
      if (_.isFunction(obj[key])) names.push(key);
    }
    return names.sort();
  };

  // Extend a given object with all the properties in passed-in object(s).
  _.extend = createAssigner(_.allKeys);

  // Assigns a given object with all the own properties in the passed-in object(s)
  // (https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/Object/assign)
  _.extendOwn = _.assign = createAssigner(_.keys);

  // Returns the first key on an object that passes a predicate test
  _.findKey = function(obj, predicate, context) {
    predicate = cb(predicate, context);
    var keys = _.keys(obj), key;
    for (var i = 0, length = keys.length; i < length; i++) {
      key = keys[i];
      if (predicate(obj[key], key, obj)) return key;
    }
  };

  // Return a copy of the object only containing the whitelisted properties.
  _.pick = function(object, oiteratee, context) {
    var result = {}, obj = object, iteratee, keys;
    if (obj == null) return result;
    if (_.isFunction(oiteratee)) {
      keys = _.allKeys(obj);
      iteratee = optimizeCb(oiteratee, context);
    } else {
      keys = flatten(arguments, false, false, 1);
      iteratee = function(value, key, obj) { return key in obj; };
      obj = Object(obj);
    }
    for (var i = 0, length = keys.length; i < length; i++) {
      var key = keys[i];
      var value = obj[key];
      if (iteratee(value, key, obj)) result[key] = value;
    }
    return result;
  };

   // Return a copy of the object without the blacklisted properties.
  _.omit = function(obj, iteratee, context) {
    if (_.isFunction(iteratee)) {
      iteratee = _.negate(iteratee);
    } else {
      var keys = _.map(flatten(arguments, false, false, 1), String);
      iteratee = function(value, key) {
        return !_.contains(keys, key);
      };
    }
    return _.pick(obj, iteratee, context);
  };

  // Fill in a given object with default properties.
  _.defaults = createAssigner(_.allKeys, true);

  // Creates an object that inherits from the given prototype object.
  // If additional properties are provided then they will be added to the
  // created object.
  _.create = function(prototype, props) {
    var result = baseCreate(prototype);
    if (props) _.extendOwn(result, props);
    return result;
  };

  // Create a (shallow-cloned) duplicate of an object.
  _.clone = function(obj) {
    if (!_.isObject(obj)) return obj;
    return _.isArray(obj) ? obj.slice() : _.extend({}, obj);
  };

  // Invokes interceptor with the obj, and then returns obj.
  // The primary purpose of this method is to "tap into" a method chain, in
  // order to perform operations on intermediate results within the chain.
  _.tap = function(obj, interceptor) {
    interceptor(obj);
    return obj;
  };

  // Returns whether an object has a given set of `key:value` pairs.
  _.isMatch = function(object, attrs) {
    var keys = _.keys(attrs), length = keys.length;
    if (object == null) return !length;
    var obj = Object(object);
    for (var i = 0; i < length; i++) {
      var key = keys[i];
      if (attrs[key] !== obj[key] || !(key in obj)) return false;
    }
    return true;
  };


  // Internal recursive comparison function for `isEqual`.
  var eq = function(a, b, aStack, bStack) {
    // Identical objects are equal. `0 === -0`, but they aren't identical.
    // See the [Harmony `egal` proposal](http://wiki.ecmascript.org/doku.php?id=harmony:egal).
    if (a === b) return a !== 0 || 1 / a === 1 / b;
    // A strict comparison is necessary because `null == undefined`.
    if (a == null || b == null) return a === b;
    // Unwrap any wrapped objects.
    if (a instanceof _) a = a._wrapped;
    if (b instanceof _) b = b._wrapped;
    // Compare `[[Class]]` names.
    var className = toString.call(a);
    if (className !== toString.call(b)) return false;
    switch (className) {
      // Strings, numbers, regular expressions, dates, and booleans are compared by value.
      case '[object RegExp]':
      // RegExps are coerced to strings for comparison (Note: '' + /a/i === '/a/i')
      case '[object String]':
        // Primitives and their corresponding object wrappers are equivalent; thus, `"5"` is
        // equivalent to `new String("5")`.
        return '' + a === '' + b;
      case '[object Number]':
        // `NaN`s are equivalent, but non-reflexive.
        // Object(NaN) is equivalent to NaN
        if (+a !== +a) return +b !== +b;
        // An `egal` comparison is performed for other numeric values.
        return +a === 0 ? 1 / +a === 1 / b : +a === +b;
      case '[object Date]':
      case '[object Boolean]':
        // Coerce dates and booleans to numeric primitive values. Dates are compared by their
        // millisecond representations. Note that invalid dates with millisecond representations
        // of `NaN` are not equivalent.
        return +a === +b;
    }

    var areArrays = className === '[object Array]';
    if (!areArrays) {
      if (typeof a != 'object' || typeof b != 'object') return false;

      // Objects with different constructors are not equivalent, but `Object`s or `Array`s
      // from different frames are.
      var aCtor = a.constructor, bCtor = b.constructor;
      if (aCtor !== bCtor && !(_.isFunction(aCtor) && aCtor instanceof aCtor &&
                               _.isFunction(bCtor) && bCtor instanceof bCtor)
                          && ('constructor' in a && 'constructor' in b)) {
        return false;
      }
    }
    // Assume equality for cyclic structures. The algorithm for detecting cyclic
    // structures is adapted from ES 5.1 section 15.12.3, abstract operation `JO`.

    // Initializing stack of traversed objects.
    // It's done here since we only need them for objects and arrays comparison.
    aStack = aStack || [];
    bStack = bStack || [];
    var length = aStack.length;
    while (length--) {
      // Linear search. Performance is inversely proportional to the number of
      // unique nested structures.
      if (aStack[length] === a) return bStack[length] === b;
    }

    // Add the first object to the stack of traversed objects.
    aStack.push(a);
    bStack.push(b);

    // Recursively compare objects and arrays.
    if (areArrays) {
      // Compare array lengths to determine if a deep comparison is necessary.
      length = a.length;
      if (length !== b.length) return false;
      // Deep compare the contents, ignoring non-numeric properties.
      while (length--) {
        if (!eq(a[length], b[length], aStack, bStack)) return false;
      }
    } else {
      // Deep compare objects.
      var keys = _.keys(a), key;
      length = keys.length;
      // Ensure that both objects contain the same number of properties before comparing deep equality.
      if (_.keys(b).length !== length) return false;
      while (length--) {
        // Deep compare each member
        key = keys[length];
        if (!(_.has(b, key) && eq(a[key], b[key], aStack, bStack))) return false;
      }
    }
    // Remove the first object from the stack of traversed objects.
    aStack.pop();
    bStack.pop();
    return true;
  };

  // Perform a deep comparison to check if two objects are equal.
  _.isEqual = function(a, b) {
    return eq(a, b);
  };

  // Is a given array, string, or object empty?
  // An "empty" object has no enumerable own-properties.
  _.isEmpty = function(obj) {
    if (obj == null) return true;
    if (isArrayLike(obj) && (_.isArray(obj) || _.isString(obj) || _.isArguments(obj))) return obj.length === 0;
    return _.keys(obj).length === 0;
  };

  // Is a given value a DOM element?
  _.isElement = function(obj) {
    return !!(obj && obj.nodeType === 1);
  };

  // Is a given value an array?
  // Delegates to ECMA5's native Array.isArray
  _.isArray = nativeIsArray || function(obj) {
    return toString.call(obj) === '[object Array]';
  };

  // Is a given variable an object?
  _.isObject = function(obj) {
    var type = typeof obj;
    return type === 'function' || type === 'object' && !!obj;
  };

  // Add some isType methods: isArguments, isFunction, isString, isNumber, isDate, isRegExp, isError.
  _.each(['Arguments', 'Function', 'String', 'Number', 'Date', 'RegExp', 'Error'], function(name) {
    _['is' + name] = function(obj) {
      return toString.call(obj) === '[object ' + name + ']';
    };
  });

  // Define a fallback version of the method in browsers (ahem, IE < 9), where
  // there isn't any inspectable "Arguments" type.
  if (!_.isArguments(arguments)) {
    _.isArguments = function(obj) {
      return _.has(obj, 'callee');
    };
  }

  // Optimize `isFunction` if appropriate. Work around some typeof bugs in old v8,
  // IE 11 (#1621), and in Safari 8 (#1929).
  if (typeof /./ != 'function' && typeof Int8Array != 'object') {
    _.isFunction = function(obj) {
      return typeof obj == 'function' || false;
    };
  }

  // Is a given object a finite number?
  _.isFinite = function(obj) {
    return isFinite(obj) && !isNaN(parseFloat(obj));
  };

  // Is the given value `NaN`? (NaN is the only number which does not equal itself).
  _.isNaN = function(obj) {
    return _.isNumber(obj) && obj !== +obj;
  };

  // Is a given value a boolean?
  _.isBoolean = function(obj) {
    return obj === true || obj === false || toString.call(obj) === '[object Boolean]';
  };

  // Is a given value equal to null?
  _.isNull = function(obj) {
    return obj === null;
  };

  // Is a given variable undefined?
  _.isUndefined = function(obj) {
    return obj === void 0;
  };

  // Shortcut function for checking if an object has a given property directly
  // on itself (in other words, not on a prototype).
  _.has = function(obj, key) {
    return obj != null && hasOwnProperty.call(obj, key);
  };

  // Utility Functions
  // -----------------

  // Run Underscore.js in *noConflict* mode, returning the `_` variable to its
  // previous owner. Returns a reference to the Underscore object.
  _.noConflict = function() {
    root._ = previousUnderscore;
    return this;
  };

  // Keep the identity function around for default iteratees.
  _.identity = function(value) {
    return value;
  };

  // Predicate-generating functions. Often useful outside of Underscore.
  _.constant = function(value) {
    return function() {
      return value;
    };
  };

  _.noop = function(){};

  _.property = property;

  // Generates a function for a given object that returns a given property.
  _.propertyOf = function(obj) {
    return obj == null ? function(){} : function(key) {
      return obj[key];
    };
  };

  // Returns a predicate for checking whether an object has a given set of
  // `key:value` pairs.
  _.matcher = _.matches = function(attrs) {
    attrs = _.extendOwn({}, attrs);
    return function(obj) {
      return _.isMatch(obj, attrs);
    };
  };

  // Run a function **n** times.
  _.times = function(n, iteratee, context) {
    var accum = Array(Math.max(0, n));
    iteratee = optimizeCb(iteratee, context, 1);
    for (var i = 0; i < n; i++) accum[i] = iteratee(i);
    return accum;
  };

  // Return a random integer between min and max (inclusive).
  _.random = function(min, max) {
    if (max == null) {
      max = min;
      min = 0;
    }
    return min + Math.floor(Math.random() * (max - min + 1));
  };

  // A (possibly faster) way to get the current timestamp as an integer.
  _.now = Date.now || function() {
    return new Date().getTime();
  };

   // List of HTML entities for escaping.
  var escapeMap = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '`': '&#x60;'
  };
  var unescapeMap = _.invert(escapeMap);

  // Functions for escaping and unescaping strings to/from HTML interpolation.
  var createEscaper = function(map) {
    var escaper = function(match) {
      return map[match];
    };
    // Regexes for identifying a key that needs to be escaped
    var source = '(?:' + _.keys(map).join('|') + ')';
    var testRegexp = RegExp(source);
    var replaceRegexp = RegExp(source, 'g');
    return function(string) {
      string = string == null ? '' : '' + string;
      return testRegexp.test(string) ? string.replace(replaceRegexp, escaper) : string;
    };
  };
  _.escape = createEscaper(escapeMap);
  _.unescape = createEscaper(unescapeMap);

  // If the value of the named `property` is a function then invoke it with the
  // `object` as context; otherwise, return it.
  _.result = function(object, property, fallback) {
    var value = object == null ? void 0 : object[property];
    if (value === void 0) {
      value = fallback;
    }
    return _.isFunction(value) ? value.call(object) : value;
  };

  // Generate a unique integer id (unique within the entire client session).
  // Useful for temporary DOM ids.
  var idCounter = 0;
  _.uniqueId = function(prefix) {
    var id = ++idCounter + '';
    return prefix ? prefix + id : id;
  };

  // By default, Underscore uses ERB-style template delimiters, change the
  // following template settings to use alternative delimiters.
  _.templateSettings = {
    evaluate    : /<%([\s\S]+?)%>/g,
    interpolate : /<%=([\s\S]+?)%>/g,
    escape      : /<%-([\s\S]+?)%>/g
  };

  // When customizing `templateSettings`, if you don't want to define an
  // interpolation, evaluation or escaping regex, we need one that is
  // guaranteed not to match.
  var noMatch = /(.)^/;

  // Certain characters need to be escaped so that they can be put into a
  // string literal.
  var escapes = {
    "'":      "'",
    '\\':     '\\',
    '\r':     'r',
    '\n':     'n',
    '\u2028': 'u2028',
    '\u2029': 'u2029'
  };

  var escaper = /\\|'|\r|\n|\u2028|\u2029/g;

  var escapeChar = function(match) {
    return '\\' + escapes[match];
  };

  // JavaScript micro-templating, similar to John Resig's implementation.
  // Underscore templating handles arbitrary delimiters, preserves whitespace,
  // and correctly escapes quotes within interpolated code.
  // NB: `oldSettings` only exists for backwards compatibility.
  _.template = function(text, settings, oldSettings) {
    if (!settings && oldSettings) settings = oldSettings;
    settings = _.defaults({}, settings, _.templateSettings);

    // Combine delimiters into one regular expression via alternation.
    var matcher = RegExp([
      (settings.escape || noMatch).source,
      (settings.interpolate || noMatch).source,
      (settings.evaluate || noMatch).source
    ].join('|') + '|$', 'g');

    // Compile the template source, escaping string literals appropriately.
    var index = 0;
    var source = "__p+='";
    text.replace(matcher, function(match, escape, interpolate, evaluate, offset) {
      source += text.slice(index, offset).replace(escaper, escapeChar);
      index = offset + match.length;

      if (escape) {
        source += "'+\n((__t=(" + escape + "))==null?'':_.escape(__t))+\n'";
      } else if (interpolate) {
        source += "'+\n((__t=(" + interpolate + "))==null?'':__t)+\n'";
      } else if (evaluate) {
        source += "';\n" + evaluate + "\n__p+='";
      }

      // Adobe VMs need the match returned to produce the correct offest.
      return match;
    });
    source += "';\n";

    // If a variable is not specified, place data values in local scope.
    if (!settings.variable) source = 'with(obj||{}){\n' + source + '}\n';

    source = "var __t,__p='',__j=Array.prototype.join," +
      "print=function(){__p+=__j.call(arguments,'');};\n" +
      source + 'return __p;\n';

    try {
      var render = new Function(settings.variable || 'obj', '_', source);
    } catch (e) {
      e.source = source;
      throw e;
    }

    var template = function(data) {
      return render.call(this, data, _);
    };

    // Provide the compiled source as a convenience for precompilation.
    var argument = settings.variable || 'obj';
    template.source = 'function(' + argument + '){\n' + source + '}';

    return template;
  };

  // Add a "chain" function. Start chaining a wrapped Underscore object.
  _.chain = function(obj) {
    var instance = _(obj);
    instance._chain = true;
    return instance;
  };

  // OOP
  // ---------------
  // If Underscore is called as a function, it returns a wrapped object that
  // can be used OO-style. This wrapper holds altered versions of all the
  // underscore functions. Wrapped objects may be chained.

  // Helper function to continue chaining intermediate results.
  var result = function(instance, obj) {
    return instance._chain ? _(obj).chain() : obj;
  };

  // Add your own custom functions to the Underscore object.
  _.mixin = function(obj) {
    _.each(_.functions(obj), function(name) {
      var func = _[name] = obj[name];
      _.prototype[name] = function() {
        var args = [this._wrapped];
        push.apply(args, arguments);
        return result(this, func.apply(_, args));
      };
    });
  };

  // Add all of the Underscore functions to the wrapper object.
  _.mixin(_);

  // Add all mutator Array functions to the wrapper.
  _.each(['pop', 'push', 'reverse', 'shift', 'sort', 'splice', 'unshift'], function(name) {
    var method = ArrayProto[name];
    _.prototype[name] = function() {
      var obj = this._wrapped;
      method.apply(obj, arguments);
      if ((name === 'shift' || name === 'splice') && obj.length === 0) delete obj[0];
      return result(this, obj);
    };
  });

  // Add all accessor Array functions to the wrapper.
  _.each(['concat', 'join', 'slice'], function(name) {
    var method = ArrayProto[name];
    _.prototype[name] = function() {
      return result(this, method.apply(this._wrapped, arguments));
    };
  });

  // Extracts the result from a wrapped and chained object.
  _.prototype.value = function() {
    return this._wrapped;
  };

  // Provide unwrapping proxy for some methods used in engine operations
  // such as arithmetic and JSON stringification.
  _.prototype.valueOf = _.prototype.toJSON = _.prototype.value;

  _.prototype.toString = function() {
    return '' + this._wrapped;
  };

  // AMD registration happens at the end for compatibility with AMD loaders
  // that may not enforce next-turn semantics on modules. Even though general
  // practice for AMD registration is to be anonymous, underscore registers
  // as a named module because, like jQuery, it is a base library that is
  // popular enough to be bundled in a third party lib, but not be part of
  // an AMD load request. Those cases could generate an error when an
  // anonymous define() is called outside of a loader request.
  if (typeof define === 'function' && define.amd) {
    define('underscore', [], function() {
      return _;
    });
  }
}.call(this));

//\u7ee7\u627f\u76f8\u5173\u903b\u8f91
(function () {

  // \u5168\u5c40\u53ef\u80fd\u7528\u5230\u7684\u53d8\u91cf
  var arr = [];
  var slice = arr.slice;
  /**
  * inherit\u65b9\u6cd5\uff0cjs\u7684\u7ee7\u627f\uff0c\u9ed8\u8ba4\u4e3a\u4e24\u4e2a\u53c2\u6570
  *
  * @param  {function} origin  \u53ef\u9009\uff0c\u8981\u7ee7\u627f\u7684\u7c7b
  * @param  {object}   methods \u88ab\u521b\u5efa\u7c7b\u7684\u6210\u5458\uff0c\u6269\u5c55\u7684\u65b9\u6cd5\u548c\u5c5e\u6027
  * @return {function}         \u7ee7\u627f\u4e4b\u540e\u7684\u5b50\u7c7b
  */
  _.inherit = function (origin, methods) {

    // \u53c2\u6570\u68c0\u6d4b\uff0c\u8be5\u7ee7\u627f\u65b9\u6cd5\uff0c\u53ea\u652f\u6301\u4e00\u4e2a\u53c2\u6570\u521b\u5efa\u7c7b\uff0c\u6216\u8005\u4e24\u4e2a\u53c2\u6570\u7ee7\u627f\u7c7b
    if (arguments.length === 0 || arguments.length > 2) throw '\u53c2\u6570\u9519\u8bef';

    var parent = null;

    // \u5c06\u53c2\u6570\u8f6c\u6362\u4e3a\u6570\u7ec4
    var properties = slice.call(arguments);

    // \u5982\u679c\u7b2c\u4e00\u4e2a\u53c2\u6570\u4e3a\u7c7b\uff08function\uff09\uff0c\u90a3\u4e48\u5c31\u5c06\u4e4b\u53d6\u51fa
    if (typeof properties[0] === 'function')
      parent = properties.shift();
    properties = properties[0];

    // \u521b\u5efa\u65b0\u7c7b\u7528\u4e8e\u8fd4\u56de
    function klass() {
      if (_.isFunction(this.initialize))
        this.initialize.apply(this, arguments);
    }

    klass.superclass = parent;

    // \u7236\u7c7b\u7684\u65b9\u6cd5\u4e0d\u505a\u4fdd\u7559\uff0c\u76f4\u63a5\u8d4b\u7ed9\u5b50\u7c7b
    // parent.subclasses = [];

    if (parent) {
      // \u4e2d\u95f4\u8fc7\u6e21\u7c7b\uff0c\u9632\u6b62parent\u7684\u6784\u9020\u51fd\u6570\u88ab\u6267\u884c
      var subclass = function () { };
      subclass.prototype = parent.prototype;
      klass.prototype = new subclass();

      // \u7236\u7c7b\u7684\u65b9\u6cd5\u4e0d\u505a\u4fdd\u7559\uff0c\u76f4\u63a5\u8d4b\u7ed9\u5b50\u7c7b
      // parent.subclasses.push(klass);
    }

    var ancestor = klass.superclass && klass.superclass.prototype;
    for (var k in properties) {
      var value = properties[k];

      //\u6ee1\u8db3\u6761\u4ef6\u5c31\u91cd\u5199
      if (ancestor && typeof value == 'function') {
        var argslist = /^\s*function\s*\(([^\(\)]*?)\)\s*?\{/i.exec(value.toString())[1].replace(/\s/g, '').split(',');
        //\u53ea\u6709\u5728\u7b2c\u4e00\u4e2a\u53c2\u6570\u4e3a$super\u60c5\u51b5\u4e0b\u624d\u9700\u8981\u5904\u7406\uff08\u662f\u5426\u5177\u6709\u91cd\u590d\u65b9\u6cd5\u9700\u8981\u7528\u6237\u81ea\u5df1\u51b3\u5b9a\uff09
        if ((argslist[0] === '$super' || argslist[0] === 't') && ancestor[k]) {
          value = (function (methodName, fn) {
            return function () {
              var scope = this;
              var args = [
                function () {
                  return ancestor[methodName].apply(scope, arguments);
                }
              ];
              return fn.apply(this, args.concat(slice.call(arguments)));
            };
          })(k, value);
        }
      }

      //\u6b64\u5904\u5bf9\u5bf9\u8c61\u8fdb\u884c\u6269\u5c55\uff0c\u5f53\u524d\u539f\u578b\u94fe\u5df2\u7ecf\u5b58\u5728\u8be5\u5bf9\u8c61\uff0c\u4fbf\u8fdb\u884c\u6269\u5c55
      if (_.isObject(klass.prototype[k]) && _.isObject(value) && (typeof klass.prototype[k] != 'function' && typeof value != 'fuction')) {
        //\u539f\u578b\u94fe\u662f\u5171\u4eab\u7684\uff0c\u8fd9\u91cc\u5904\u7406\u903b\u8f91\u8981\u6539
        var temp = {};
        _.extend(temp, klass.prototype[k]);
        _.extend(temp, value);
        klass.prototype[k] = temp;
      } else {
        klass.prototype[k] = value;
      }

    }

    if (!klass.prototype.initialize)
      klass.prototype.initialize = function () { };

    klass.prototype.constructor = klass;

    return klass;
  };

})();

//\u57fa\u7840\u65b9\u6cd5
(function () {

  _.removeAllSpace = function (str) {
    return str.replace(/\s+/g, "");
  };

  })();

//flip\u624b\u52bf\u5de5\u5177
(function () {

    //\u504f\u79fb\u6b65\u957f
    var step = 20;

    var touch = {};
    var down = 'touchstart';
    var move = 'touchmove';
    var up = 'touchend';
    if (!('ontouchstart' in window)) {
      down = 'mousedown';
      move = 'mousemove';
      up = 'mouseup';
    }

    //\u7b80\u5355\u501f\u9274ccd\u601d\u7ef4\u505a\u7b80\u8981\u5904\u7406
    function swipeDirection(x1, x2, y1, y2, sensibility) {

      //x\u79fb\u52a8\u7684\u6b65\u957f
      var _x = Math.abs(x1 - x2);
      //y\u79fb\u52a8\u6b65\u957f
      var _y = Math.abs(y1 - y2);
      var dir = _x >= _y ? (x1 - x2 > 0 ? 'left' : 'right') : (y1 - y2 > 0 ? 'up' : 'down');

      //\u8bbe\u7f6e\u7075\u654f\u5ea6\u9650\u5236
      if (sensibility) {
        if (dir == 'left' || dir == 'right') {
          if ((_y / _x) > sensibility) dir = '';
        } else if (dir == 'up' || dir == 'down') {
          if ((_x / _y) > sensibility) dir = '';
        }
      }
      return dir;
    }

    //sensibility\u8bbe\u7f6e\u7075\u654f\u5ea6\uff0c\u503c\u4e3a0-1
    function flip(el, dir, fn, noDefault, sensibility) {
      if (!el) return;

      el.on(down, function (e) {
        var pos = (e.touches && e.touches[0]) || e;
        touch.x1 = pos.pageX;
        touch.y1 = pos.pageY;

      }).on(move, function (e) {
        var pos = (e.touches && e.touches[0]) || e;
        touch.x2 = pos.pageX;
        touch.y2 = pos.pageY;

        //\u5982\u679cview\u8fc7\u957f\u6ed1\u4e0d\u52a8\u662f\u6709\u95ee\u9898\u7684
        if (!noDefault) { e.preventDefault(); }
      }).on(up, function (e) {


        if ((touch.x2 && Math.abs(touch.x1 - touch.x2) > step) ||
        (touch.y2 && Math.abs(touch.y1 - touch.y2) > step)) {
          var _dir = swipeDirection(touch.x1, touch.x2, touch.y1, touch.y2, sensibility);
          if (dir === _dir) {
            typeof fn == 'function' && fn();
          }
        } else {
          //tap\u7684\u60c5\u51b5
          if (dir === 'tap') {
            typeof fn == 'function' && fn();
          }
        }
      });
    }

    function flipDestroy(el) {
      if (!el) return;
      el.off(down).off(move).off(up);
    }

    _.flip = flip;
    _.flipDestroy = flipDestroy;

  })();

//\u65e5\u671f\u64cd\u4f5c\u7c7b
(function () {

  /**
  * @description \u9759\u6001\u65e5\u671f\u64cd\u4f5c\u7c7b\uff0c\u5c01\u88c5\u7cfb\u5217\u65e5\u671f\u64cd\u4f5c\u65b9\u6cd5
  * @description \u8f93\u5165\u65f6\u5019\u6708\u4efd\u81ea\u52a8\u51cf\u4e00\uff0c\u8f93\u51fa\u65f6\u5019\u81ea\u52a8\u52a0\u4e00
  * @return {object} \u8fd4\u56de\u64cd\u4f5c\u65b9\u6cd5
  */
  _.dateUtil = {
    /**
    * @description \u6570\u5b57\u64cd\u4f5c\uff0c
    * @return {string} \u8fd4\u56de\u5904\u7406\u540e\u7684\u6570\u5b57
    */
    formatNum: function (n) {
      if (n < 10) return '0' + n;
      return n;
    },
    /**
    * @description \u5c06\u5b57\u7b26\u4e32\u8f6c\u6362\u4e3a\u65e5\u671f\uff0c\u652f\u6301\u683c\u5f0fy-m-d ymd (y m r)\u4ee5\u53ca\u6807\u51c6\u7684
    * @return {Date} \u8fd4\u56de\u65e5\u671f\u5bf9\u8c61
    */
    parse: function (dateStr, formatStr) {
      if (typeof dateStr === 'undefined') return null;
      if (typeof formatStr === 'string') {
        var _d = new Date(formatStr);
        //\u9996\u5148\u53d6\u5f97\u987a\u5e8f\u76f8\u5173\u5b57\u7b26\u4e32
        var arrStr = formatStr.replace(/[^ymd]/g, '').split('');
        if (!arrStr && arrStr.length != 3) return null;

        var formatStr = formatStr.replace(/y|m|d/g, function (k) {
          switch (k) {
            case 'y': return '(\\d{4})';
            case 'm': ;
            case 'd': return '(\\d{1,2})';
          }
        });

        var reg = new RegExp(formatStr, 'g');
        var arr = reg.exec(dateStr)

        var dateObj = {};
        for (var i = 0, len = arrStr.length; i < len; i++) {
          dateObj[arrStr[i]] = arr[i + 1];
        }
        return new Date(dateObj['y'], dateObj['m'] - 1, dateObj['d']);
      }
      return null;
    },
    /**
    * @description\u5c06\u65e5\u671f\u683c\u5f0f\u5316\u4e3a\u5b57\u7b26\u4e32
    * @return {string} \u5e38\u7528\u683c\u5f0f\u5316\u5b57\u7b26\u4e32
    */
    format: function (date, format) {
      if (arguments.length < 2 && !date.getTime) {
        format = date;
        date = new Date();
      }
      typeof format != 'string' && (format = 'Y\u5e74M\u6708D\u65e5 H\u65f6F\u5206S\u79d2');
      return format.replace(/Y|y|M|m|D|d|H|h|F|f|S|s/g, function (a) {
        switch (a) {
          case "y": return (date.getFullYear() + "").slice(2);
          case "Y": return date.getFullYear();
          case "m": return date.getMonth() + 1;
          case "M": return _.dateUtil.formatNum(date.getMonth() + 1);
          case "d": return date.getDate();
          case "D": return _.dateUtil.formatNum(date.getDate());
          case "h": return date.getHours();
          case "H": return _.dateUtil.formatNum(date.getHours());
          case "f": return date.getMinutes();
          case "F": return _.dateUtil.formatNum(date.getMinutes());
          case "s": return date.getSeconds();
          case "S": return _.dateUtil.formatNum(date.getSeconds());
        }
      });
    },
    // @description \u662f\u5426\u4e3a\u4e3a\u65e5\u671f\u5bf9\u8c61\uff0c\u8be5\u65b9\u6cd5\u53ef\u80fd\u6709\u5751\uff0c\u4f7f\u7528\u9700\u8981\u614e\u91cd
    // @param year {num} \u65e5\u671f\u5bf9\u8c61
    // @return {boolean} \u8fd4\u56de\u503c
    isDate: function (d) {
      if ((typeof d == 'object') && (d instanceof Date)) return true;
      return false;
    },
    // @description \u662f\u5426\u4e3a\u95f0\u5e74
    // @param year {num} \u53ef\u80fd\u662f\u5e74\u4efd\u6216\u8005\u4e3a\u4e00\u4e2adate\u65f6\u95f4
    // @return {boolean} \u8fd4\u56de\u503c
    isLeapYear: function (year) {
      //\u4f20\u5165\u4e3a\u65f6\u95f4\u683c\u5f0f\u9700\u8981\u5904\u7406
      if (_.dateUtil.isDate(year)) year = year.getFullYear()
      if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) return true;
      return false;
    },

    // @description \u83b7\u53d6\u4e00\u4e2a\u6708\u4efd\u7684\u5929\u6570
    // @param year {num} \u53ef\u80fd\u662f\u5e74\u4efd\u6216\u8005\u4e3a\u4e00\u4e2adate\u65f6\u95f4
    // @param year {num} \u6708\u4efd
    // @return {num} \u8fd4\u56de\u5929\u6570
    getDaysOfMonth: function (year, month) {
      //\u81ea\u52a8\u51cf\u4e00\u4ee5\u4fbf\u64cd\u4f5c
      month--;
      if (_.dateUtil.isDate(year)) {
        month = year.getMonth(); //\u6ce8\u610f\u6b64\u5904\u6708\u4efd\u8981\u52a01\uff0c\u6240\u4ee5\u6211\u4eec\u8981\u51cf\u4e00
        year = year.getFullYear();
      }
      return [31, _.dateUtil.isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month];
    },

    // @description \u83b7\u53d6\u4e00\u4e2a\u6708\u4efd1\u53f7\u662f\u661f\u671f\u51e0\uff0c\u6ce8\u610f\u6b64\u65f6\u7684\u6708\u4efd\u4f20\u5165\u65f6\u9700\u8981\u81ea\u4e3b\u51cf\u4e00
    // @param year {num} \u53ef\u80fd\u662f\u5e74\u4efd\u6216\u8005\u4e3a\u4e00\u4e2adate\u65f6\u95f4
    // @param year {num} \u6708\u4efd
    // @return {num} \u5f53\u6708\u4e00\u53f7\u4e3a\u661f\u671f\u51e00-6
    getBeginDayOfMouth: function (year, month) {
      //\u81ea\u52a8\u51cf\u4e00\u4ee5\u4fbf\u64cd\u4f5c
      month--;
      if ((typeof year == 'object') && (year instanceof Date)) {
        month = year.getMonth();
        year = year.getFullYear();
      }
      var d = new Date(year, month, 1);
      return d.getDay();
    }
  };

})();

//\u5c01\u88c5setTimeout
(function() {

  var TimerRes = {};

  _.setInterval = function(fn, timeout, ns) {
    if (!ns) ns = 'g';
    if (!TimerRes[ns]) TimerRes[ns] = [];
    TimerRes[ns].push(setInterval(fn, timeout));
  };

  _.clearInterval = function (rid, ns) {
    var k, v, k1, i, len, i1, len1, resArr, j;

    if(typeof rid == 'number'){
      //1 clearInterval\uff0c \u6e05\u9664\u6570\u7ec4
      for(k in TimerRes) {
        v = TimerRes[k];
        for(i = 0, len = v.length; i < len; i++) {
          if(rid == v[i]) {
            v.splice(i, 1)
            clearInterval(rid);
            return;
          }
        }
      }
    }

    if(typeof rid == 'string'){ 
      ns = rid;
      resArr = TimerRes[ns];
      j = resArr.length;
      while(j != 0){
       _.clearInterval(resArr[resArr.length - 1]);
      }
    }

    if(arguments.length == 0) {
       for(k1 in TimerRes) {
       _.clearInterval(k1);     
       }
    }
    
  }

})();
define("underscore_extend", function(){});

/*! iScroll v5.1.1 ~ (c) 2008-2014 Matteo Spinelli ~ http://cubiq.org/license */
(function (window, document, Math) {
var rAF = window.requestAnimationFrame  ||
    window.webkitRequestAnimationFrame  ||
    window.mozRequestAnimationFrame     ||
    window.oRequestAnimationFrame       ||
    window.msRequestAnimationFrame      ||
    function (callback) { window.setTimeout(callback, 1000 / 60); };

var utils = (function () {
    var me = {};

    var _elementStyle = document.createElement('div').style;
    var _vendor = (function () {
        var vendors = ['t', 'webkitT', 'MozT', 'msT', 'OT'],
            transform,
            i = 0,
            l = vendors.length;

        for ( ; i < l; i++ ) {
            transform = vendors[i] + 'ransform';
            if ( transform in _elementStyle ) return vendors[i].substr(0, vendors[i].length-1);
        }

        return false;
    })();

    function _prefixStyle (style) {
        if ( _vendor === false ) return false;
        if ( _vendor === '' ) return style;
        return _vendor + style.charAt(0).toUpperCase() + style.substr(1);
    }

    me.getTime = Date.now || function getTime () { return new Date().getTime(); };

    me.extend = function (target, obj) {
        for ( var i in obj ) {
            target[i] = obj[i];
        }
    };

    me.addEvent = function (el, type, fn, capture) {
        el.addEventListener(type, fn, !!capture);
    };

    me.removeEvent = function (el, type, fn, capture) {
        el.removeEventListener(type, fn, !!capture);
    };

    me.momentum = function (current, start, time, lowerMargin, wrapperSize, deceleration) {
        var distance = current - start,
            speed = Math.abs(distance) / time,
            destination,
            duration;

        deceleration = deceleration === undefined ? 0.0006 : deceleration;

        destination = current + ( speed * speed ) / ( 2 * deceleration ) * ( distance < 0 ? -1 : 1 );
        duration = speed / deceleration;

        if ( destination < lowerMargin ) {
            destination = wrapperSize ? lowerMargin - ( wrapperSize / 2.5 * ( speed / 8 ) ) : lowerMargin;
            distance = Math.abs(destination - current);
            duration = distance / speed;
        } else if ( destination > 0 ) {
            destination = wrapperSize ? wrapperSize / 2.5 * ( speed / 8 ) : 0;
            distance = Math.abs(current) + destination;
            duration = distance / speed;
        }

        return {
            destination: Math.round(destination),
            duration: duration
        };
    };

    var _transform = _prefixStyle('transform');

    me.extend(me, {
        hasTransform: _transform !== false,
        hasPerspective: _prefixStyle('perspective') in _elementStyle,
        hasTouch: 'ontouchstart' in window,
        hasPointer: navigator.msPointerEnabled,
        hasTransition: _prefixStyle('transition') in _elementStyle
    });

    // This should find all Android browsers lower than build 535.19 (both stock browser and webview)
    me.isBadAndroid = /Android /.test(window.navigator.appVersion) && !(/Chrome\/\d/.test(window.navigator.appVersion));

    me.extend(me.style = {}, {
        transform: _transform,
        transitionTimingFunction: _prefixStyle('transitionTimingFunction'),
        transitionDuration: _prefixStyle('transitionDuration'),
        transitionDelay: _prefixStyle('transitionDelay'),
        transformOrigin: _prefixStyle('transformOrigin')
    });

    me.hasClass = function (e, c) {
        var re = new RegExp("(^|\\s)" + c + "(\\s|$)");
        return re.test(e.className);
    };

    me.addClass = function (e, c) {
        if ( me.hasClass(e, c) ) {
            return;
        }

        var newclass = e.className.split(' ');
        newclass.push(c);
        e.className = newclass.join(' ');
    };

    me.removeClass = function (e, c) {
        if ( !me.hasClass(e, c) ) {
            return;
        }

        var re = new RegExp("(^|\\s)" + c + "(\\s|$)", 'g');
        e.className = e.className.replace(re, ' ');
    };

    me.offset = function (el) {
        var left = -el.offsetLeft,
            top = -el.offsetTop;

        // jshint -W084
        while (el = el.offsetParent) {
            left -= el.offsetLeft;
            top -= el.offsetTop;
        }
        // jshint +W084

        return {
            left: left,
            top: top
        };
    };

    me.preventDefaultException = function (el, exceptions) {
        for ( var i in exceptions ) {
            if ( exceptions[i].test(el[i]) ) {
                return true;
            }
        }

        return false;
    };

    me.extend(me.eventType = {}, {
        touchstart: 1,
        touchmove: 1,
        touchend: 1,

        mousedown: 2,
        mousemove: 2,
        mouseup: 2,

        MSPointerDown: 3,
        MSPointerMove: 3,
        MSPointerUp: 3
    });

    me.extend(me.ease = {}, {
        quadratic: {
            style: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)',
            fn: function (k) {
                return k * ( 2 - k );
            }
        },
        circular: {
            style: 'cubic-bezier(0.1, 0.57, 0.1, 1)',   // Not properly "circular" but this looks better, it should be (0.075, 0.82, 0.165, 1)
            fn: function (k) {
                return Math.sqrt( 1 - ( --k * k ) );
            }
        },
        back: {
            style: 'cubic-bezier(0.175, 0.885, 0.32, 1.275)',
            fn: function (k) {
                var b = 4;
                return ( k = k - 1 ) * k * ( ( b + 1 ) * k + b ) + 1;
            }
        },
        bounce: {
            style: '',
            fn: function (k) {
                if ( ( k /= 1 ) < ( 1 / 2.75 ) ) {
                    return 7.5625 * k * k;
                } else if ( k < ( 2 / 2.75 ) ) {
                    return 7.5625 * ( k -= ( 1.5 / 2.75 ) ) * k + 0.75;
                } else if ( k < ( 2.5 / 2.75 ) ) {
                    return 7.5625 * ( k -= ( 2.25 / 2.75 ) ) * k + 0.9375;
                } else {
                    return 7.5625 * ( k -= ( 2.625 / 2.75 ) ) * k + 0.984375;
                }
            }
        },
        elastic: {
            style: '',
            fn: function (k) {
                var f = 0.22,
                    e = 0.4;

                if ( k === 0 ) { return 0; }
                if ( k == 1 ) { return 1; }

                return ( e * Math.pow( 2, - 10 * k ) * Math.sin( ( k - f / 4 ) * ( 2 * Math.PI ) / f ) + 1 );
            }
        }
    });

    me.tap = function (e, eventName) {
        var ev = document.createEvent('Event');
        ev.initEvent(eventName, true, true);
        ev.pageX = e.pageX;
        ev.pageY = e.pageY;
        e.target.dispatchEvent(ev);
    };

    me.click = function (e) {
        var target = e.target,
            ev;

        if ( !(/(SELECT|INPUT|TEXTAREA)/i).test(target.tagName) ) {
            ev = document.createEvent('MouseEvents');
            ev.initMouseEvent('click', true, true, e.view, 1,
                target.screenX, target.screenY, target.clientX, target.clientY,
                e.ctrlKey, e.altKey, e.shiftKey, e.metaKey,
                0, null);

            ev._constructed = true;
            target.dispatchEvent(ev);
        }
    };

    return me;
})();

function IScroll (el, options) {
    this.wrapper = typeof el == 'string' ? document.querySelector(el) : el;
    this.scroller = this.wrapper.children[0];
    this.scrollerStyle = this.scroller.style;       // cache style for better performance

    this.options = {

        resizeScrollbars: true,

        mouseWheelSpeed: 20,

        snapThreshold: 0.334,

// INSERT POINT: OPTIONS 

        startX: 0,
        startY: 0,
        scrollY: true,
        directionLockThreshold: 5,
        momentum: true,

        bounce: true,
        bounceTime: 600,
        bounceEasing: '',

        preventDefault: true,
        preventDefaultException: { tagName: /^(INPUT|TEXTAREA|BUTTON|SELECT)$/ },

        HWCompositing: true,
        useTransition: true,
        useTransform: true
    };

    for ( var i in options ) {
        this.options[i] = options[i];
    }

    // Normalize options
    this.translateZ = this.options.HWCompositing && utils.hasPerspective ? ' translateZ(0)' : '';

    this.options.useTransition = utils.hasTransition && this.options.useTransition;
    this.options.useTransform = utils.hasTransform && this.options.useTransform;

    this.options.eventPassthrough = this.options.eventPassthrough === true ? 'vertical' : this.options.eventPassthrough;
    this.options.preventDefault = !this.options.eventPassthrough && this.options.preventDefault;

    // If you want eventPassthrough I have to lock one of the axes
    this.options.scrollY = this.options.eventPassthrough == 'vertical' ? false : this.options.scrollY;
    this.options.scrollX = this.options.eventPassthrough == 'horizontal' ? false : this.options.scrollX;

    // With eventPassthrough we also need lockDirection mechanism
    this.options.freeScroll = this.options.freeScroll && !this.options.eventPassthrough;
    this.options.directionLockThreshold = this.options.eventPassthrough ? 0 : this.options.directionLockThreshold;

    this.options.bounceEasing = typeof this.options.bounceEasing == 'string' ? utils.ease[this.options.bounceEasing] || utils.ease.circular : this.options.bounceEasing;

    this.options.resizePolling = this.options.resizePolling === undefined ? 60 : this.options.resizePolling;

    if ( this.options.tap === true ) {
        this.options.tap = 'tap';
    }

    if ( this.options.shrinkScrollbars == 'scale' ) {
        this.options.useTransition = false;
    }

    this.options.invertWheelDirection = this.options.invertWheelDirection ? -1 : 1;

    if ( this.options.probeType == 3 ) {
        this.options.useTransition = false; }

// INSERT POINT: NORMALIZATION

    // Some defaults    
    this.x = 0;
    this.y = 0;
    this.directionX = 0;
    this.directionY = 0;
    this._events = {};

// INSERT POINT: DEFAULTS

    this._init();
    this.refresh();

    this.scrollTo(this.options.startX, this.options.startY);
    this.enable();
}

IScroll.prototype = {
    version: '5.1.1',

    _init: function () {
        this._initEvents();

        if ( this.options.scrollbars || this.options.indicators ) {
            this._initIndicators();
        }

        if ( this.options.mouseWheel ) {
            this._initWheel();
        }

        if ( this.options.snap ) {
            this._initSnap();
        }

        if ( this.options.keyBindings ) {
            this._initKeys();
        }

// INSERT POINT: _init

    },

    destroy: function () {
        this._initEvents(true);

        this._execEvent('destroy');
    },

    _transitionEnd: function (e) {
        if ( e.target != this.scroller || !this.isInTransition ) {
            return;
        }

        this._transitionTime();
        if ( !this.resetPosition(this.options.bounceTime) ) {
            this.isInTransition = false;
            this._execEvent('scrollEnd');
        }
    },

    _start: function (e) {
        // React to left mouse button only
        if ( utils.eventType[e.type] != 1 ) {
            if ( e.button !== 0 ) {
                return;
            }
        }

        if ( !this.enabled || (this.initiated && utils.eventType[e.type] !== this.initiated) ) {
            return;
        }

        if ( this.options.preventDefault && !utils.isBadAndroid && !utils.preventDefaultException(e.target, this.options.preventDefaultException) ) {
            e.preventDefault();
        }

        var point = e.touches ? e.touches[0] : e,
            pos;

        this.initiated  = utils.eventType[e.type];
        this.moved      = false;
        this.distX      = 0;
        this.distY      = 0;
        this.directionX = 0;
        this.directionY = 0;
        this.directionLocked = 0;

        this._transitionTime();

        this.startTime = utils.getTime();

        if ( this.options.useTransition && this.isInTransition ) {
            this.isInTransition = false;
            pos = this.getComputedPosition();
            this._translate(Math.round(pos.x), Math.round(pos.y));
            this._execEvent('scrollEnd');
        } else if ( !this.options.useTransition && this.isAnimating ) {
            this.isAnimating = false;
            this._execEvent('scrollEnd');
        }

        this.startX    = this.x;
        this.startY    = this.y;
        this.absStartX = this.x;
        this.absStartY = this.y;
        this.pointX    = point.pageX;
        this.pointY    = point.pageY;

        this._execEvent('beforeScrollStart');
    },

    _move: function (e) {
        if ( !this.enabled || utils.eventType[e.type] !== this.initiated ) {
            return;
        }

        if ( this.options.preventDefault ) {    // increases performance on Android? TODO: check!
            e.preventDefault();
        }

        var point       = e.touches ? e.touches[0] : e,
            deltaX      = point.pageX - this.pointX,
            deltaY      = point.pageY - this.pointY,
            timestamp   = utils.getTime(),
            newX, newY,
            absDistX, absDistY;

        this.pointX     = point.pageX;
        this.pointY     = point.pageY;

        this.distX      += deltaX;
        this.distY      += deltaY;
        absDistX        = Math.abs(this.distX);
        absDistY        = Math.abs(this.distY);

        // We need to move at least 10 pixels for the scrolling to initiate
        if ( timestamp - this.endTime > 300 && (absDistX < 10 && absDistY < 10) ) {
            return;
        }

        // If you are scrolling in one direction lock the other
        if ( !this.directionLocked && !this.options.freeScroll ) {
            if ( absDistX > absDistY + this.options.directionLockThreshold ) {
                this.directionLocked = 'h';     // lock horizontally
            } else if ( absDistY >= absDistX + this.options.directionLockThreshold ) {
                this.directionLocked = 'v';     // lock vertically
            } else {
                this.directionLocked = 'n';     // no lock
            }
        }

        if ( this.directionLocked == 'h' ) {
            if ( this.options.eventPassthrough == 'vertical' ) {
                e.preventDefault();
            } else if ( this.options.eventPassthrough == 'horizontal' ) {
                this.initiated = false;
                return;
            }

            deltaY = 0;
        } else if ( this.directionLocked == 'v' ) {
            if ( this.options.eventPassthrough == 'horizontal' ) {
                e.preventDefault();
            } else if ( this.options.eventPassthrough == 'vertical' ) {
                this.initiated = false;
                return;
            }

            deltaX = 0;
        }

        deltaX = this.hasHorizontalScroll ? deltaX : 0;
        deltaY = this.hasVerticalScroll ? deltaY : 0;

        newX = this.x + deltaX;
        newY = this.y + deltaY;

        // Slow down if outside of the boundaries
        if ( newX > 0 || newX < this.maxScrollX ) {
            newX = this.options.bounce ? this.x + deltaX / 3 : newX > 0 ? 0 : this.maxScrollX;
        }
        if ( newY > 0 || newY < this.maxScrollY ) {
            newY = this.options.bounce ? this.y + deltaY / 3 : newY > 0 ? 0 : this.maxScrollY;
        }

        this.directionX = deltaX > 0 ? -1 : deltaX < 0 ? 1 : 0;
        this.directionY = deltaY > 0 ? -1 : deltaY < 0 ? 1 : 0;

        if ( !this.moved ) {
            this._execEvent('scrollStart');
        }

        this.moved = true;

        this._translate(newX, newY);

/* REPLACE START: _move */
        if ( timestamp - this.startTime > 300 ) {
            this.startTime = timestamp;
            this.startX = this.x;
            this.startY = this.y;

            if ( this.options.probeType == 1 ) {
                this._execEvent('scroll');
            }
        }

        if ( this.options.probeType > 1 ) {
            this._execEvent('scroll');
        }
/* REPLACE END: _move */

    },

    _end: function (e) {
        if ( !this.enabled || utils.eventType[e.type] !== this.initiated ) {
            return;
        }

        if ( this.options.preventDefault && !utils.preventDefaultException(e.target, this.options.preventDefaultException) ) {
            e.preventDefault();
        }

        var point = e.changedTouches ? e.changedTouches[0] : e,
            momentumX,
            momentumY,
            duration = utils.getTime() - this.startTime,
            newX = Math.round(this.x),
            newY = Math.round(this.y),
            distanceX = Math.abs(newX - this.startX),
            distanceY = Math.abs(newY - this.startY),
            time = 0,
            easing = '';

        this.isInTransition = 0;
        this.initiated = 0;
        this.endTime = utils.getTime();

        // reset if we are outside of the boundaries
        if ( this.resetPosition(this.options.bounceTime) ) {
            return;
        }

        this.scrollTo(newX, newY);  // ensures that the last position is rounded

        // we scrolled less than 10 pixels
        if ( !this.moved ) {
            if ( this.options.tap ) {
                utils.tap(e, this.options.tap);
            }

            if ( this.options.click ) {
                utils.click(e);
            }

            this._execEvent('scrollCancel');
            return;
        }

        if ( this._events.flick && duration < 200 && distanceX < 100 && distanceY < 100 ) {
            this._execEvent('flick');
            return;
        }

        // start momentum animation if needed
        if ( this.options.momentum && duration < 300 ) {
            momentumX = this.hasHorizontalScroll ? utils.momentum(this.x, this.startX, duration, this.maxScrollX, this.options.bounce ? this.wrapperWidth : 0, this.options.deceleration) : { destination: newX, duration: 0 };
            momentumY = this.hasVerticalScroll ? utils.momentum(this.y, this.startY, duration, this.maxScrollY, this.options.bounce ? this.wrapperHeight : 0, this.options.deceleration) : { destination: newY, duration: 0 };
            newX = momentumX.destination;
            newY = momentumY.destination;
            time = Math.max(momentumX.duration, momentumY.duration);
            this.isInTransition = 1;
        }


        if ( this.options.snap ) {
            var snap = this._nearestSnap(newX, newY);
            this.currentPage = snap;
            time = this.options.snapSpeed || Math.max(
                    Math.max(
                        Math.min(Math.abs(newX - snap.x), 1000),
                        Math.min(Math.abs(newY - snap.y), 1000)
                    ), 300);
            newX = snap.x;
            newY = snap.y;

            this.directionX = 0;
            this.directionY = 0;
            easing = this.options.bounceEasing;
        }

// INSERT POINT: _end

        if ( newX != this.x || newY != this.y ) {
            // change easing function when scroller goes out of the boundaries
            if ( newX > 0 || newX < this.maxScrollX || newY > 0 || newY < this.maxScrollY ) {
                easing = utils.ease.quadratic;
            }

            this.scrollTo(newX, newY, time, easing);
            return;
        }

        this._execEvent('scrollEnd');
    },

    _resize: function () {
        var that = this;

        clearTimeout(this.resizeTimeout);

        this.resizeTimeout = setTimeout(function () {
            that.refresh();
        }, this.options.resizePolling);
    },

    resetPosition: function (time) {
        var x = this.x,
            y = this.y;

        time = time || 0;

        if ( !this.hasHorizontalScroll || this.x > 0 ) {
            x = 0;
        } else if ( this.x < this.maxScrollX ) {
            x = this.maxScrollX;
        }

        if ( !this.hasVerticalScroll || this.y > 0 ) {
            y = 0;
        } else if ( this.y < this.maxScrollY ) {
            y = this.maxScrollY;
        }

        if ( x == this.x && y == this.y ) {
            return false;
        }

        this.scrollTo(x, y, time, this.options.bounceEasing);

        return true;
    },

    disable: function () {
        this.enabled = false;
    },

    enable: function () {
        this.enabled = true;
    },

    refresh: function () {
        var rf = this.wrapper.offsetHeight;     // Force reflow

        this.wrapperWidth   = this.wrapper.clientWidth;
        this.wrapperHeight  = this.wrapper.clientHeight;

/* REPLACE START: refresh */

        this.scrollerWidth  = this.scroller.offsetWidth;
        this.scrollerHeight = this.scroller.offsetHeight;

        this.maxScrollX     = this.wrapperWidth - this.scrollerWidth;
        this.maxScrollY     = this.wrapperHeight - this.scrollerHeight;

/* REPLACE END: refresh */

        this.hasHorizontalScroll    = this.options.scrollX && this.maxScrollX < 0;
        this.hasVerticalScroll      = this.options.scrollY && this.maxScrollY < 0;

        if ( !this.hasHorizontalScroll ) {
            this.maxScrollX = 0;
            this.scrollerWidth = this.wrapperWidth;
        }

        if ( !this.hasVerticalScroll ) {
            this.maxScrollY = 0;
            this.scrollerHeight = this.wrapperHeight;
        }

        this.endTime = 0;
        this.directionX = 0;
        this.directionY = 0;

        this.wrapperOffset = utils.offset(this.wrapper);

        this._execEvent('refresh');

        this.resetPosition();

// INSERT POINT: _refresh

    },

    on: function (type, fn) {
        if ( !this._events[type] ) {
            this._events[type] = [];
        }

        this._events[type].push(fn);
    },

    off: function (type, fn) {
        if ( !this._events[type] ) {
            return;
        }

        var index = this._events[type].indexOf(fn);

        if ( index > -1 ) {
            this._events[type].splice(index, 1);
        }
    },

    _execEvent: function (type) {
        if ( !this._events[type] ) {
            return;
        }

        var i = 0,
            l = this._events[type].length;

        if ( !l ) {
            return;
        }

        for ( ; i < l; i++ ) {
            this._events[type][i].apply(this, [].slice.call(arguments, 1));
        }
    },

    scrollBy: function (x, y, time, easing) {
        x = this.x + x;
        y = this.y + y;
        time = time || 0;

        this.scrollTo(x, y, time, easing);
    },

    scrollTo: function (x, y, time, easing) {
        easing = easing || utils.ease.circular;

        this.isInTransition = this.options.useTransition && time > 0;

        if ( !time || (this.options.useTransition && easing.style) ) {
            this._transitionTimingFunction(easing.style);
            this._transitionTime(time);
            this._translate(x, y);
        } else {
            this._animate(x, y, time, easing.fn);
        }
    },

    scrollToElement: function (el, time, offsetX, offsetY, easing) {
        el = el.nodeType ? el : this.scroller.querySelector(el);

        if ( !el ) {
            return;
        }

        var pos = utils.offset(el);

        pos.left -= this.wrapperOffset.left;
        pos.top  -= this.wrapperOffset.top;

        // if offsetX/Y are true we center the element to the screen
        if ( offsetX === true ) {
            offsetX = Math.round(el.offsetWidth / 2 - this.wrapper.offsetWidth / 2);
        }
        if ( offsetY === true ) {
            offsetY = Math.round(el.offsetHeight / 2 - this.wrapper.offsetHeight / 2);
        }

        pos.left -= offsetX || 0;
        pos.top  -= offsetY || 0;

        pos.left = pos.left > 0 ? 0 : pos.left < this.maxScrollX ? this.maxScrollX : pos.left;
        pos.top  = pos.top  > 0 ? 0 : pos.top  < this.maxScrollY ? this.maxScrollY : pos.top;

        time = time === undefined || time === null || time === 'auto' ? Math.max(Math.abs(this.x-pos.left), Math.abs(this.y-pos.top)) : time;

        this.scrollTo(pos.left, pos.top, time, easing);
    },

    _transitionTime: function (time) {
        time = time || 0;

        this.scrollerStyle[utils.style.transitionDuration] = time + 'ms';

        if ( !time && utils.isBadAndroid ) {
            this.scrollerStyle[utils.style.transitionDuration] = '0.001s';
        }


        if ( this.indicators ) {
            for ( var i = this.indicators.length; i--; ) {
                this.indicators[i].transitionTime(time);
            }
        }


// INSERT POINT: _transitionTime

    },

    _transitionTimingFunction: function (easing) {
        this.scrollerStyle[utils.style.transitionTimingFunction] = easing;


        if ( this.indicators ) {
            for ( var i = this.indicators.length; i--; ) {
                this.indicators[i].transitionTimingFunction(easing);
            }
        }


// INSERT POINT: _transitionTimingFunction

    },

    _translate: function (x, y) {
        if ( this.options.useTransform ) {

/* REPLACE START: _translate */

            this.scrollerStyle[utils.style.transform] = 'translate(' + x + 'px,' + y + 'px)' + this.translateZ;

/* REPLACE END: _translate */

        } else {
            x = Math.round(x);
            y = Math.round(y);
            this.scrollerStyle.left = x + 'px';
            this.scrollerStyle.top = y + 'px';
        }

        this.x = x;
        this.y = y;


    if ( this.indicators ) {
        for ( var i = this.indicators.length; i--; ) {
            this.indicators[i].updatePosition();
        }
    }


// INSERT POINT: _translate

    },

    _initEvents: function (remove) {
        var eventType = remove ? utils.removeEvent : utils.addEvent,
            target = this.options.bindToWrapper ? this.wrapper : window;

        eventType(window, 'orientationchange', this);
        eventType(window, 'resize', this);

        if ( this.options.click ) {
            eventType(this.wrapper, 'click', this, true);
        }

        if ( !this.options.disableMouse ) {
            eventType(this.wrapper, 'mousedown', this);
            eventType(target, 'mousemove', this);
            eventType(target, 'mousecancel', this);
            eventType(target, 'mouseup', this);
        }

        if ( utils.hasPointer && !this.options.disablePointer ) {
            eventType(this.wrapper, 'MSPointerDown', this);
            eventType(target, 'MSPointerMove', this);
            eventType(target, 'MSPointerCancel', this);
            eventType(target, 'MSPointerUp', this);
        }

        if ( utils.hasTouch && !this.options.disableTouch ) {
            eventType(this.wrapper, 'touchstart', this);
            eventType(target, 'touchmove', this);
            eventType(target, 'touchcancel', this);
            eventType(target, 'touchend', this);
        }

        eventType(this.scroller, 'transitionend', this);
        eventType(this.scroller, 'webkitTransitionEnd', this);
        eventType(this.scroller, 'oTransitionEnd', this);
        eventType(this.scroller, 'MSTransitionEnd', this);
    },

    getComputedPosition: function () {
        var matrix = window.getComputedStyle(this.scroller, null),
            x, y;

        if ( this.options.useTransform ) {
            matrix = matrix[utils.style.transform].split(')')[0].split(', ');
            x = +(matrix[12] || matrix[4]);
            y = +(matrix[13] || matrix[5]);
        } else {
            x = +matrix.left.replace(/[^-\d.]/g, '');
            y = +matrix.top.replace(/[^-\d.]/g, '');
        }

        return { x: x, y: y };
    },

    _initIndicators: function () {
        var interactive = this.options.interactiveScrollbars,
            customStyle = typeof this.options.scrollbars != 'string',
            indicators = [],
            indicator;

        var that = this;

        this.indicators = [];

        if ( this.options.scrollbars ) {
            // Vertical scrollbar
            if ( this.options.scrollY ) {
                indicator = {
                    el: createDefaultScrollbar('v', interactive, this.options.scrollbars),
                    interactive: interactive,
                    defaultScrollbars: true,
                    customStyle: customStyle,
                    resize: this.options.resizeScrollbars,
                    shrink: this.options.shrinkScrollbars,
                    fade: this.options.fadeScrollbars,
                    listenX: false
                };

                this.wrapper.appendChild(indicator.el);
                indicators.push(indicator);
            }

            // Horizontal scrollbar
            if ( this.options.scrollX ) {
                indicator = {
                    el: createDefaultScrollbar('h', interactive, this.options.scrollbars),
                    interactive: interactive,
                    defaultScrollbars: true,
                    customStyle: customStyle,
                    resize: this.options.resizeScrollbars,
                    shrink: this.options.shrinkScrollbars,
                    fade: this.options.fadeScrollbars,
                    listenY: false
                };

                this.wrapper.appendChild(indicator.el);
                indicators.push(indicator);
            }
        }

        if ( this.options.indicators ) {
            // TODO: check concat compatibility
            indicators = indicators.concat(this.options.indicators);
        }

        for ( var i = indicators.length; i--; ) {
            this.indicators.push( new Indicator(this, indicators[i]) );
        }

        // TODO: check if we can use array.map (wide compatibility and performance issues)
        function _indicatorsMap (fn) {
            for ( var i = that.indicators.length; i--; ) {
                fn.call(that.indicators[i]);
            }
        }

        if ( this.options.fadeScrollbars ) {
            this.on('scrollEnd', function () {
                _indicatorsMap(function () {
                    this.fade();
                });
            });

            this.on('scrollCancel', function () {
                _indicatorsMap(function () {
                    this.fade();
                });
            });

            this.on('scrollStart', function () {
                _indicatorsMap(function () {
                    this.fade(1);
                });
            });

            this.on('beforeScrollStart', function () {
                _indicatorsMap(function () {
                    this.fade(1, true);
                });
            });
        }


        this.on('refresh', function () {
            _indicatorsMap(function () {
                this.refresh();
            });
        });

        this.on('destroy', function () {
            _indicatorsMap(function () {
                this.destroy();
            });

            delete this.indicators;
        });
    },

    _initWheel: function () {
        utils.addEvent(this.wrapper, 'wheel', this);
        utils.addEvent(this.wrapper, 'mousewheel', this);
        utils.addEvent(this.wrapper, 'DOMMouseScroll', this);

        this.on('destroy', function () {
            utils.removeEvent(this.wrapper, 'wheel', this);
            utils.removeEvent(this.wrapper, 'mousewheel', this);
            utils.removeEvent(this.wrapper, 'DOMMouseScroll', this);
        });
    },

    _wheel: function (e) {
        if ( !this.enabled ) {
            return;
        }

        e.preventDefault();
        e.stopPropagation();

        var wheelDeltaX, wheelDeltaY,
            newX, newY,
            that = this;

        if ( this.wheelTimeout === undefined ) {
            that._execEvent('scrollStart');
        }

        // Execute the scrollEnd event after 400ms the wheel stopped scrolling
        clearTimeout(this.wheelTimeout);
        this.wheelTimeout = setTimeout(function () {
            that._execEvent('scrollEnd');
            that.wheelTimeout = undefined;
        }, 400);

        if ( 'deltaX' in e ) {
            wheelDeltaX = -e.deltaX;
            wheelDeltaY = -e.deltaY;
        } else if ( 'wheelDeltaX' in e ) {
            wheelDeltaX = e.wheelDeltaX / 120 * this.options.mouseWheelSpeed;
            wheelDeltaY = e.wheelDeltaY / 120 * this.options.mouseWheelSpeed;
        } else if ( 'wheelDelta' in e ) {
            wheelDeltaX = wheelDeltaY = e.wheelDelta / 120 * this.options.mouseWheelSpeed;
        } else if ( 'detail' in e ) {
            wheelDeltaX = wheelDeltaY = -e.detail / 3 * this.options.mouseWheelSpeed;
        } else {
            return;
        }

        wheelDeltaX *= this.options.invertWheelDirection;
        wheelDeltaY *= this.options.invertWheelDirection;

        if ( !this.hasVerticalScroll ) {
            wheelDeltaX = wheelDeltaY;
            wheelDeltaY = 0;
        }

        if ( this.options.snap ) {
            newX = this.currentPage.pageX;
            newY = this.currentPage.pageY;

            if ( wheelDeltaX > 0 ) {
                newX--;
            } else if ( wheelDeltaX < 0 ) {
                newX++;
            }

            if ( wheelDeltaY > 0 ) {
                newY--;
            } else if ( wheelDeltaY < 0 ) {
                newY++;
            }

            this.goToPage(newX, newY);

            return;
        }

        newX = this.x + Math.round(this.hasHorizontalScroll ? wheelDeltaX : 0);
        newY = this.y + Math.round(this.hasVerticalScroll ? wheelDeltaY : 0);

        if ( newX > 0 ) {
            newX = 0;
        } else if ( newX < this.maxScrollX ) {
            newX = this.maxScrollX;
        }

        if ( newY > 0 ) {
            newY = 0;
        } else if ( newY < this.maxScrollY ) {
            newY = this.maxScrollY;
        }

        this.scrollTo(newX, newY, 0);

        if ( this.options.probeType > 1 ) {
            this._execEvent('scroll');
        }

// INSERT POINT: _wheel
    },

    _initSnap: function () {
        this.currentPage = {};

        if ( typeof this.options.snap == 'string' ) {
            this.options.snap = this.scroller.querySelectorAll(this.options.snap);
        }

        this.on('refresh', function () {
            var i = 0, l,
                m = 0, n,
                cx, cy,
                x = 0, y,
                stepX = this.options.snapStepX || this.wrapperWidth,
                stepY = this.options.snapStepY || this.wrapperHeight,
                el;

            this.pages = [];

            if ( !this.wrapperWidth || !this.wrapperHeight || !this.scrollerWidth || !this.scrollerHeight ) {
                return;
            }

            if ( this.options.snap === true ) {
                cx = Math.round( stepX / 2 );
                cy = Math.round( stepY / 2 );

                while ( x > -this.scrollerWidth ) {
                    this.pages[i] = [];
                    l = 0;
                    y = 0;

                    while ( y > -this.scrollerHeight ) {
                        this.pages[i][l] = {
                            x: Math.max(x, this.maxScrollX),
                            y: Math.max(y, this.maxScrollY),
                            width: stepX,
                            height: stepY,
                            cx: x - cx,
                            cy: y - cy
                        };

                        y -= stepY;
                        l++;
                    }

                    x -= stepX;
                    i++;
                }
            } else {
                el = this.options.snap;
                l = el.length;
                n = -1;

                for ( ; i < l; i++ ) {
                    if ( i === 0 || el[i].offsetLeft <= el[i-1].offsetLeft ) {
                        m = 0;
                        n++;
                    }

                    if ( !this.pages[m] ) {
                        this.pages[m] = [];
                    }

                    x = Math.max(-el[i].offsetLeft, this.maxScrollX);
                    y = Math.max(-el[i].offsetTop, this.maxScrollY);
                    cx = x - Math.round(el[i].offsetWidth / 2);
                    cy = y - Math.round(el[i].offsetHeight / 2);

                    this.pages[m][n] = {
                        x: x,
                        y: y,
                        width: el[i].offsetWidth,
                        height: el[i].offsetHeight,
                        cx: cx,
                        cy: cy
                    };

                    if ( x > this.maxScrollX ) {
                        m++;
                    }
                }
            }

            this.goToPage(this.currentPage.pageX || 0, this.currentPage.pageY || 0, 0);

            // Update snap threshold if needed
            if ( this.options.snapThreshold % 1 === 0 ) {
                this.snapThresholdX = this.options.snapThreshold;
                this.snapThresholdY = this.options.snapThreshold;
            } else {
                this.snapThresholdX = Math.round(this.pages[this.currentPage.pageX][this.currentPage.pageY].width * this.options.snapThreshold);
                this.snapThresholdY = Math.round(this.pages[this.currentPage.pageX][this.currentPage.pageY].height * this.options.snapThreshold);
            }
        });

        this.on('flick', function () {
            var time = this.options.snapSpeed || Math.max(
                    Math.max(
                        Math.min(Math.abs(this.x - this.startX), 1000),
                        Math.min(Math.abs(this.y - this.startY), 1000)
                    ), 300);

            this.goToPage(
                this.currentPage.pageX + this.directionX,
                this.currentPage.pageY + this.directionY,
                time
            );
        });
    },

    _nearestSnap: function (x, y) {
        if ( !this.pages.length ) {
            return { x: 0, y: 0, pageX: 0, pageY: 0 };
        }

        var i = 0,
            l = this.pages.length,
            m = 0;

        // Check if we exceeded the snap threshold
        if ( Math.abs(x - this.absStartX) < this.snapThresholdX &&
            Math.abs(y - this.absStartY) < this.snapThresholdY ) {
            return this.currentPage;
        }

        if ( x > 0 ) {
            x = 0;
        } else if ( x < this.maxScrollX ) {
            x = this.maxScrollX;
        }

        if ( y > 0 ) {
            y = 0;
        } else if ( y < this.maxScrollY ) {
            y = this.maxScrollY;
        }

        for ( ; i < l; i++ ) {
            if ( x >= this.pages[i][0].cx ) {
                x = this.pages[i][0].x;
                break;
            }
        }

        l = this.pages[i].length;

        for ( ; m < l; m++ ) {
            if ( y >= this.pages[0][m].cy ) {
                y = this.pages[0][m].y;
                break;
            }
        }

        if ( i == this.currentPage.pageX ) {
            i += this.directionX;

            if ( i < 0 ) {
                i = 0;
            } else if ( i >= this.pages.length ) {
                i = this.pages.length - 1;
            }

            x = this.pages[i][0].x;
        }

        if ( m == this.currentPage.pageY ) {
            m += this.directionY;

            if ( m < 0 ) {
                m = 0;
            } else if ( m >= this.pages[0].length ) {
                m = this.pages[0].length - 1;
            }

            y = this.pages[0][m].y;
        }

        return {
            x: x,
            y: y,
            pageX: i,
            pageY: m
        };
    },

    goToPage: function (x, y, time, easing) {
        easing = easing || this.options.bounceEasing;

        if ( x >= this.pages.length ) {
            x = this.pages.length - 1;
        } else if ( x < 0 ) {
            x = 0;
        }

        if ( y >= this.pages[x].length ) {
            y = this.pages[x].length - 1;
        } else if ( y < 0 ) {
            y = 0;
        }

        var posX = this.pages[x][y].x,
            posY = this.pages[x][y].y;

        time = time === undefined ? this.options.snapSpeed || Math.max(
            Math.max(
                Math.min(Math.abs(posX - this.x), 1000),
                Math.min(Math.abs(posY - this.y), 1000)
            ), 300) : time;

        this.currentPage = {
            x: posX,
            y: posY,
            pageX: x,
            pageY: y
        };

        this.scrollTo(posX, posY, time, easing);
    },

    next: function (time, easing) {
        var x = this.currentPage.pageX,
            y = this.currentPage.pageY;

        x++;

        if ( x >= this.pages.length && this.hasVerticalScroll ) {
            x = 0;
            y++;
        }

        this.goToPage(x, y, time, easing);
    },

    prev: function (time, easing) {
        var x = this.currentPage.pageX,
            y = this.currentPage.pageY;

        x--;

        if ( x < 0 && this.hasVerticalScroll ) {
            x = 0;
            y--;
        }

        this.goToPage(x, y, time, easing);
    },

    _initKeys: function (e) {
        // default key bindings
        var keys = {
            pageUp: 33,
            pageDown: 34,
            end: 35,
            home: 36,
            left: 37,
            up: 38,
            right: 39,
            down: 40
        };
        var i;

        // if you give me characters I give you keycode
        if ( typeof this.options.keyBindings == 'object' ) {
            for ( i in this.options.keyBindings ) {
                if ( typeof this.options.keyBindings[i] == 'string' ) {
                    this.options.keyBindings[i] = this.options.keyBindings[i].toUpperCase().charCodeAt(0);
                }
            }
        } else {
            this.options.keyBindings = {};
        }

        for ( i in keys ) {
            this.options.keyBindings[i] = this.options.keyBindings[i] || keys[i];
        }

        utils.addEvent(window, 'keydown', this);

        this.on('destroy', function () {
            utils.removeEvent(window, 'keydown', this);
        });
    },

    _key: function (e) {
        if ( !this.enabled ) {
            return;
        }

        var snap = this.options.snap,   // we are using this alot, better to cache it
            newX = snap ? this.currentPage.pageX : this.x,
            newY = snap ? this.currentPage.pageY : this.y,
            now = utils.getTime(),
            prevTime = this.keyTime || 0,
            acceleration = 0.250,
            pos;

        if ( this.options.useTransition && this.isInTransition ) {
            pos = this.getComputedPosition();

            this._translate(Math.round(pos.x), Math.round(pos.y));
            this.isInTransition = false;
        }

        this.keyAcceleration = now - prevTime < 200 ? Math.min(this.keyAcceleration + acceleration, 50) : 0;

        switch ( e.keyCode ) {
            case this.options.keyBindings.pageUp:
                if ( this.hasHorizontalScroll && !this.hasVerticalScroll ) {
                    newX += snap ? 1 : this.wrapperWidth;
                } else {
                    newY += snap ? 1 : this.wrapperHeight;
                }
                break;
            case this.options.keyBindings.pageDown:
                if ( this.hasHorizontalScroll && !this.hasVerticalScroll ) {
                    newX -= snap ? 1 : this.wrapperWidth;
                } else {
                    newY -= snap ? 1 : this.wrapperHeight;
                }
                break;
            case this.options.keyBindings.end:
                newX = snap ? this.pages.length-1 : this.maxScrollX;
                newY = snap ? this.pages[0].length-1 : this.maxScrollY;
                break;
            case this.options.keyBindings.home:
                newX = 0;
                newY = 0;
                break;
            case this.options.keyBindings.left:
                newX += snap ? -1 : 5 + this.keyAcceleration>>0;
                break;
            case this.options.keyBindings.up:
                newY += snap ? 1 : 5 + this.keyAcceleration>>0;
                break;
            case this.options.keyBindings.right:
                newX -= snap ? -1 : 5 + this.keyAcceleration>>0;
                break;
            case this.options.keyBindings.down:
                newY -= snap ? 1 : 5 + this.keyAcceleration>>0;
                break;
            default:
                return;
        }

        if ( snap ) {
            this.goToPage(newX, newY);
            return;
        }

        if ( newX > 0 ) {
            newX = 0;
            this.keyAcceleration = 0;
        } else if ( newX < this.maxScrollX ) {
            newX = this.maxScrollX;
            this.keyAcceleration = 0;
        }

        if ( newY > 0 ) {
            newY = 0;
            this.keyAcceleration = 0;
        } else if ( newY < this.maxScrollY ) {
            newY = this.maxScrollY;
            this.keyAcceleration = 0;
        }

        this.scrollTo(newX, newY, 0);

        this.keyTime = now;
    },

    _animate: function (destX, destY, duration, easingFn) {
        var that = this,
            startX = this.x,
            startY = this.y,
            startTime = utils.getTime(),
            destTime = startTime + duration;

        function step () {
            var now = utils.getTime(),
                newX, newY,
                easing;

            if ( now >= destTime ) {
                that.isAnimating = false;
                that._translate(destX, destY);
                
                if ( !that.resetPosition(that.options.bounceTime) ) {
                    that._execEvent('scrollEnd');
                }

                return;
            }

            now = ( now - startTime ) / duration;
            easing = easingFn(now);
            newX = ( destX - startX ) * easing + startX;
            newY = ( destY - startY ) * easing + startY;
            that._translate(newX, newY);

            if ( that.isAnimating ) {
                rAF(step);
            }

            if ( that.options.probeType == 3 ) {
                that._execEvent('scroll');
            }
        }

        this.isAnimating = true;
        step();
    },

    handleEvent: function (e) {
        switch ( e.type ) {
            case 'touchstart':
            case 'MSPointerDown':
            case 'mousedown':
                this._start(e);
                break;
            case 'touchmove':
            case 'MSPointerMove':
            case 'mousemove':
                this._move(e);
                break;
            case 'touchend':
            case 'MSPointerUp':
            case 'mouseup':
            case 'touchcancel':
            case 'MSPointerCancel':
            case 'mousecancel':
                this._end(e);
                break;
            case 'orientationchange':
            case 'resize':
                this._resize();
                break;
            case 'transitionend':
            case 'webkitTransitionEnd':
            case 'oTransitionEnd':
            case 'MSTransitionEnd':
                this._transitionEnd(e);
                break;
            case 'wheel':
            case 'DOMMouseScroll':
            case 'mousewheel':
                this._wheel(e);
                break;
            case 'keydown':
                this._key(e);
                break;
            case 'click':
                if ( !e._constructed ) {
                    e.preventDefault();
                    e.stopPropagation();
                }
                break;
        }
    }
};
function createDefaultScrollbar (direction, interactive, type) {
    var scrollbar = document.createElement('div'),
        indicator = document.createElement('div');

    if ( type === true ) {
        scrollbar.style.cssText = 'position:absolute;z-index:9999';
        indicator.style.cssText = '-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;position:absolute;background:rgba(0,0,0,0.5);border:1px solid rgba(255,255,255,0.9);border-radius:3px';
    }

    indicator.className = 'iScrollIndicator';

    if ( direction == 'h' ) {
        if ( type === true ) {
            scrollbar.style.cssText += ';height:7px;left:2px;right:2px;bottom:0';
            indicator.style.height = '100%';
        }
        scrollbar.className = 'iScrollHorizontalScrollbar';
    } else {
        if ( type === true ) {
            scrollbar.style.cssText += ';width:7px;bottom:2px;top:2px;right:1px';
            indicator.style.width = '100%';
        }
        scrollbar.className = 'iScrollVerticalScrollbar';
    }

    scrollbar.style.cssText += ';overflow:hidden';

    if ( !interactive ) {
        scrollbar.style.pointerEvents = 'none';
    }

    scrollbar.appendChild(indicator);

    return scrollbar;
}

function Indicator (scroller, options) {
    this.wrapper = typeof options.el == 'string' ? document.querySelector(options.el) : options.el;
    this.wrapperStyle = this.wrapper.style;
    this.indicator = this.wrapper.children[0];
    this.indicatorStyle = this.indicator.style;
    this.scroller = scroller;

    this.options = {
        listenX: true,
        listenY: true,
        interactive: false,
        resize: true,
        defaultScrollbars: false,
        shrink: false,
        fade: false,
        speedRatioX: 0,
        speedRatioY: 0
    };

    for ( var i in options ) {
        this.options[i] = options[i];
    }

    this.sizeRatioX = 1;
    this.sizeRatioY = 1;
    this.maxPosX = 0;
    this.maxPosY = 0;

    if ( this.options.interactive ) {
        if ( !this.options.disableTouch ) {
            utils.addEvent(this.indicator, 'touchstart', this);
            utils.addEvent(window, 'touchend', this);
        }
        if ( !this.options.disablePointer ) {
            utils.addEvent(this.indicator, 'MSPointerDown', this);
            utils.addEvent(window, 'MSPointerUp', this);
        }
        if ( !this.options.disableMouse ) {
            utils.addEvent(this.indicator, 'mousedown', this);
            utils.addEvent(window, 'mouseup', this);
        }
    }

    if ( this.options.fade ) {
        this.wrapperStyle[utils.style.transform] = this.scroller.translateZ;
        this.wrapperStyle[utils.style.transitionDuration] = utils.isBadAndroid ? '0.001s' : '0ms';
        this.wrapperStyle.opacity = '0';
    }
}

Indicator.prototype = {
    handleEvent: function (e) {
        switch ( e.type ) {
            case 'touchstart':
            case 'MSPointerDown':
            case 'mousedown':
                this._start(e);
                break;
            case 'touchmove':
            case 'MSPointerMove':
            case 'mousemove':
                this._move(e);
                break;
            case 'touchend':
            case 'MSPointerUp':
            case 'mouseup':
            case 'touchcancel':
            case 'MSPointerCancel':
            case 'mousecancel':
                this._end(e);
                break;
        }
    },

    destroy: function () {
        if ( this.options.interactive ) {
            utils.removeEvent(this.indicator, 'touchstart', this);
            utils.removeEvent(this.indicator, 'MSPointerDown', this);
            utils.removeEvent(this.indicator, 'mousedown', this);

            utils.removeEvent(window, 'touchmove', this);
            utils.removeEvent(window, 'MSPointerMove', this);
            utils.removeEvent(window, 'mousemove', this);

            utils.removeEvent(window, 'touchend', this);
            utils.removeEvent(window, 'MSPointerUp', this);
            utils.removeEvent(window, 'mouseup', this);
        }

        if ( this.options.defaultScrollbars ) {
            this.wrapper.parentNode.removeChild(this.wrapper);
        }
    },

    _start: function (e) {
        var point = e.touches ? e.touches[0] : e;

        e.preventDefault();
        e.stopPropagation();

        this.transitionTime();

        this.initiated = true;
        this.moved = false;
        this.lastPointX = point.pageX;
        this.lastPointY = point.pageY;

        this.startTime  = utils.getTime();

        if ( !this.options.disableTouch ) {
            utils.addEvent(window, 'touchmove', this);
        }
        if ( !this.options.disablePointer ) {
            utils.addEvent(window, 'MSPointerMove', this);
        }
        if ( !this.options.disableMouse ) {
            utils.addEvent(window, 'mousemove', this);
        }

        this.scroller._execEvent('beforeScrollStart');
    },

    _move: function (e) {
        var point = e.touches ? e.touches[0] : e,
            deltaX, deltaY,
            newX, newY,
            timestamp = utils.getTime();

        if ( !this.moved ) {
            this.scroller._execEvent('scrollStart');
        }

        this.moved = true;

        deltaX = point.pageX - this.lastPointX;
        this.lastPointX = point.pageX;

        deltaY = point.pageY - this.lastPointY;
        this.lastPointY = point.pageY;

        newX = this.x + deltaX;
        newY = this.y + deltaY;

        this._pos(newX, newY);


        if ( this.scroller.options.probeType == 1 && timestamp - this.startTime > 300 ) {
            this.startTime = timestamp;
            this.scroller._execEvent('scroll');
        } else if ( this.scroller.options.probeType > 1 ) {
            this.scroller._execEvent('scroll');
        }


// INSERT POINT: indicator._move

        e.preventDefault();
        e.stopPropagation();
    },

    _end: function (e) {
        if ( !this.initiated ) {
            return;
        }

        this.initiated = false;

        e.preventDefault();
        e.stopPropagation();

        utils.removeEvent(window, 'touchmove', this);
        utils.removeEvent(window, 'MSPointerMove', this);
        utils.removeEvent(window, 'mousemove', this);

        if ( this.scroller.options.snap ) {
            var snap = this.scroller._nearestSnap(this.scroller.x, this.scroller.y);

            var time = this.options.snapSpeed || Math.max(
                    Math.max(
                        Math.min(Math.abs(this.scroller.x - snap.x), 1000),
                        Math.min(Math.abs(this.scroller.y - snap.y), 1000)
                    ), 300);

            if ( this.scroller.x != snap.x || this.scroller.y != snap.y ) {
                this.scroller.directionX = 0;
                this.scroller.directionY = 0;
                this.scroller.currentPage = snap;
                this.scroller.scrollTo(snap.x, snap.y, time, this.scroller.options.bounceEasing);
            }
        }

        if ( this.moved ) {
            this.scroller._execEvent('scrollEnd');
        }
    },

    transitionTime: function (time) {
        time = time || 0;
        this.indicatorStyle[utils.style.transitionDuration] = time + 'ms';

        if ( !time && utils.isBadAndroid ) {
            this.indicatorStyle[utils.style.transitionDuration] = '0.001s';
        }
    },

    transitionTimingFunction: function (easing) {
        this.indicatorStyle[utils.style.transitionTimingFunction] = easing;
    },

    refresh: function () {
        this.transitionTime();

        if ( this.options.listenX && !this.options.listenY ) {
            this.indicatorStyle.display = this.scroller.hasHorizontalScroll ? 'block' : 'none';
        } else if ( this.options.listenY && !this.options.listenX ) {
            this.indicatorStyle.display = this.scroller.hasVerticalScroll ? 'block' : 'none';
        } else {
            this.indicatorStyle.display = this.scroller.hasHorizontalScroll || this.scroller.hasVerticalScroll ? 'block' : 'none';
        }

        if ( this.scroller.hasHorizontalScroll && this.scroller.hasVerticalScroll ) {
            utils.addClass(this.wrapper, 'iScrollBothScrollbars');
            utils.removeClass(this.wrapper, 'iScrollLoneScrollbar');

            if ( this.options.defaultScrollbars && this.options.customStyle ) {
                if ( this.options.listenX ) {
                    this.wrapper.style.right = '8px';
                } else {
                    this.wrapper.style.bottom = '8px';
                }
            }
        } else {
            utils.removeClass(this.wrapper, 'iScrollBothScrollbars');
            utils.addClass(this.wrapper, 'iScrollLoneScrollbar');

            if ( this.options.defaultScrollbars && this.options.customStyle ) {
                if ( this.options.listenX ) {
                    this.wrapper.style.right = '2px';
                } else {
                    this.wrapper.style.bottom = '2px';
                }
            }
        }

        var r = this.wrapper.offsetHeight;  // force refresh

        if ( this.options.listenX ) {
            this.wrapperWidth = this.wrapper.clientWidth;
            if ( this.options.resize ) {
                this.indicatorWidth = Math.max(Math.round(this.wrapperWidth * this.wrapperWidth / (this.scroller.scrollerWidth || this.wrapperWidth || 1)), 8);
                this.indicatorStyle.width = this.indicatorWidth + 'px';
            } else {
                this.indicatorWidth = this.indicator.clientWidth;
            }

            this.maxPosX = this.wrapperWidth - this.indicatorWidth;

            if ( this.options.shrink == 'clip' ) {
                this.minBoundaryX = -this.indicatorWidth + 8;
                this.maxBoundaryX = this.wrapperWidth - 8;
            } else {
                this.minBoundaryX = 0;
                this.maxBoundaryX = this.maxPosX;
            }

            this.sizeRatioX = this.options.speedRatioX || (this.scroller.maxScrollX && (this.maxPosX / this.scroller.maxScrollX));  
        }

        if ( this.options.listenY ) {
            this.wrapperHeight = this.wrapper.clientHeight;
            if ( this.options.resize ) {
                this.indicatorHeight = Math.max(Math.round(this.wrapperHeight * this.wrapperHeight / (this.scroller.scrollerHeight || this.wrapperHeight || 1)), 8);
                this.indicatorStyle.height = this.indicatorHeight + 'px';
            } else {
                this.indicatorHeight = this.indicator.clientHeight;
            }

            this.maxPosY = this.wrapperHeight - this.indicatorHeight;

            if ( this.options.shrink == 'clip' ) {
                this.minBoundaryY = -this.indicatorHeight + 8;
                this.maxBoundaryY = this.wrapperHeight - 8;
            } else {
                this.minBoundaryY = 0;
                this.maxBoundaryY = this.maxPosY;
            }

            this.maxPosY = this.wrapperHeight - this.indicatorHeight;
            this.sizeRatioY = this.options.speedRatioY || (this.scroller.maxScrollY && (this.maxPosY / this.scroller.maxScrollY));
        }

        this.updatePosition();
    },

    updatePosition: function () {
        var x = this.options.listenX && Math.round(this.sizeRatioX * this.scroller.x) || 0,
            y = this.options.listenY && Math.round(this.sizeRatioY * this.scroller.y) || 0;

        if ( !this.options.ignoreBoundaries ) {
            if ( x < this.minBoundaryX ) {
                if ( this.options.shrink == 'scale' ) {
                    this.width = Math.max(this.indicatorWidth + x, 8);
                    this.indicatorStyle.width = this.width + 'px';
                }
                x = this.minBoundaryX;
            } else if ( x > this.maxBoundaryX ) {
                if ( this.options.shrink == 'scale' ) {
                    this.width = Math.max(this.indicatorWidth - (x - this.maxPosX), 8);
                    this.indicatorStyle.width = this.width + 'px';
                    x = this.maxPosX + this.indicatorWidth - this.width;
                } else {
                    x = this.maxBoundaryX;
                }
            } else if ( this.options.shrink == 'scale' && this.width != this.indicatorWidth ) {
                this.width = this.indicatorWidth;
                this.indicatorStyle.width = this.width + 'px';
            }

            if ( y < this.minBoundaryY ) {
                if ( this.options.shrink == 'scale' ) {
                    this.height = Math.max(this.indicatorHeight + y * 3, 8);
                    this.indicatorStyle.height = this.height + 'px';
                }
                y = this.minBoundaryY;
            } else if ( y > this.maxBoundaryY ) {
                if ( this.options.shrink == 'scale' ) {
                    this.height = Math.max(this.indicatorHeight - (y - this.maxPosY) * 3, 8);
                    this.indicatorStyle.height = this.height + 'px';
                    y = this.maxPosY + this.indicatorHeight - this.height;
                } else {
                    y = this.maxBoundaryY;
                }
            } else if ( this.options.shrink == 'scale' && this.height != this.indicatorHeight ) {
                this.height = this.indicatorHeight;
                this.indicatorStyle.height = this.height + 'px';
            }
        }

        this.x = x;
        this.y = y;

        if ( this.scroller.options.useTransform ) {
            this.indicatorStyle[utils.style.transform] = 'translate(' + x + 'px,' + y + 'px)' + this.scroller.translateZ;
        } else {
            this.indicatorStyle.left = x + 'px';
            this.indicatorStyle.top = y + 'px';
        }
    },

    _pos: function (x, y) {
        if ( x < 0 ) {
            x = 0;
        } else if ( x > this.maxPosX ) {
            x = this.maxPosX;
        }

        if ( y < 0 ) {
            y = 0;
        } else if ( y > this.maxPosY ) {
            y = this.maxPosY;
        }

        x = this.options.listenX ? Math.round(x / this.sizeRatioX) : this.scroller.x;
        y = this.options.listenY ? Math.round(y / this.sizeRatioY) : this.scroller.y;

        this.scroller.scrollTo(x, y);
    },

    fade: function (val, hold) {
        if ( hold && !this.visible ) {
            return;
        }

        clearTimeout(this.fadeTimeout);
        this.fadeTimeout = null;

        var time = val ? 250 : 500,
            delay = val ? 0 : 300;

        val = val ? '1' : '0';

        this.wrapperStyle[utils.style.transitionDuration] = time + 'ms';

        this.fadeTimeout = setTimeout((function (val) {
            this.wrapperStyle.opacity = val;
            this.visible = +val;
        }).bind(this, val), delay);
    }
};

IScroll.utils = utils;

if ( typeof module != 'undefined' && module.exports ) {
    module.exports = IScroll;
} else {
    window.IScroll = IScroll;
}

})(window, document, Math);
define("IScroll", (function (global) {
    return function () {
        var ret, fn;
        return ret || global.IScroll;
    };
}(this)));

define('_sdk_tool',[],function(){
    var _modalTemplateTempDiv = document.createElement('div');

    var toString = Object.prototype.toString;

    function dump_object(obj) {
        var buff, prop;
        buff = [];
        for (prop in obj) {
            buff.push(dump_to_string(prop) + ': ' + dump_to_string(obj[prop]))
        }
        return '{' + buff.join(', ') + '}';
    }

    function dump_array(arr) {
        var buff, i, len;
        buff = [];
        for (i=0, len=arr.length; i<len; i++) {
            buff.push(dump_to_string(arr[i]));
        }
        return '[' + buff.join(', ') + ']';
    }

    function dump_to_string(obj) {
        if (toString.call(obj) == '[object Function]') {
            return obj.toString();
        } else if (toString.call(obj) == '[object Array]') {
            return dump_array(obj);
        } else if (toString.call(obj) == '[object String]') {
            return '"' + obj.replace('"', '\\"') + '"';
        } else if (obj === Object(obj)) {
            return dump_object(obj);
        }
        return obj.toString();
    }


    var tool = {
        modal:function  (params) {
            params = params || {};
            var modalHTML = '';

            var buttonsHTML = '';
            if (params.buttons && params.buttons.length > 0) {
                for (var i = 0; i < params.buttons.length; i++) {
                    buttonsHTML += '<span class="modal-button' + (params.buttons[i].bold ? ' modal-button-bold' : '') + '">' + params.buttons[i].text + '</span>';
                }
            }
            var titleHTML = params.title ? '<div class="modal-title">' + params.title + '</div>' : '';
            var textHTML = params.text ? '<div class="modal-text">' + params.text + '</div>' : '';
            var afterTextHTML = params.afterText ? params.afterText : '';
            var noButtons = !params.buttons || params.buttons.length === 0 ? 'modal-no-buttons' : '';
            var modalCls = params.modalCls || '';
            modalHTML = '<div class="modal ' + modalCls + ' ' + noButtons + '"><div class="modal-inner">' + (titleHTML + textHTML + afterTextHTML) + '</div><div class="modal-buttons">' + buttonsHTML + '</div></div>';


            _modalTemplateTempDiv.innerHTML = modalHTML;

            var modal = $(_modalTemplateTempDiv).children();

            $('body').append(modal[0]);

            // Add events on buttons
            modal.find('.modal-button').each(function (index, el) {
                $(el).on('click', function (e) {
                    if (params.buttons[index].close !== false) tool.closeModal(modal);
                    if (params.buttons[index].onClick) params.buttons[index].onClick(modal, e);
                    if (params.onClick) params.onClick(modal, index);
                });
            });
            tool.openModal(modal);
            return modal[0];
        },
        alert : function (text, title, callbackOk) {
            if (typeof title === 'function') {
                callbackOk = arguments[1];
                title = undefined;
            }
            return tool.modal({
                text: text || '',
                title: typeof title === 'undefined' ? '\u63d0\u793a' : title,
                buttons: [ {text: '\u786e\u5b9a', bold: true, onClick: callbackOk} ]
            });
        },
        openModal : function (modal) {
            modal = $(modal);

            var isPopover = modal.hasClass('popover');
            var isPopup = modal.hasClass('popup');
            var isLoginScreen = modal.hasClass('login-screen');
            if (!isPopover && !isPopup && !isLoginScreen) modal.css({marginTop: - Math.round(modal.outerHeight() / 2) + 'px'});

            var overlay;
            if (!isLoginScreen) {
                if ($('.modal-overlay').length === 0 && !isPopup) {
                    $('body').append('<div class="modal-overlay"></div>');
                }
                if ($('.popup-overlay').length === 0 && isPopup) {
                    $('body').append('<div class="popup-overlay"></div>');
                }
                overlay = isPopup ? $('.popup-overlay') : $('.modal-overlay');
            }

            //Make sure that styles are applied, trigger relayout;
            var clientLeft = modal[0].clientLeft;

            // Trugger open event
            modal.trigger('open');

            // Classes for transition in
            if (!isLoginScreen) overlay.addClass('modal-overlay-visible');
            modal.removeClass('modal-out').addClass('modal-in').show()
            setTimeout(function () {
                if (modal.hasClass('modal-out')) modal.trigger('closed');
                else modal.trigger('opened');
            },300);
            return true;
        },
        closeModal:function  () {
            $('.modal,.modal-overlay').remove()
        },
        urlParam:function  (str) {
            str = str || window.location.search; //\u83b7\u53d6url\u4e2d"?"\u7b26\u540e\u7684\u5b57\u4e32
            if (typeof str !== 'string') {
                return {};
            }

            str = str.trim().replace(/^(\?|#|&)/, '');

            if (!str) {
                return {};
            }

            return str.split('&').reduce(function (ret, param) {
                var parts = param.replace(/\+/g, ' ').split('=');
                var key = parts[0];
                var val = parts[1];

                key = decodeURIComponent(key);
                // missing `=` should be `null`:
                // http://w3.org/TR/2012/WD-url-20120524/#collect-url-parameters
                val = val === undefined ? null : decodeURIComponent(val);

                if (!ret.hasOwnProperty(key)) {
                    ret[key] = val;
                } else if (Array.isArray(ret[key])) {
                    ret[key].push(val);
                } else {
                    ret[key] = [ret[key], val];
                }

                return ret;
            }, {});
        },
        //\u4ece\u67d0\u4e2a\u65f6\u95f4\u70b9\u5f80\u524d\u63a8\u5bfcbeginSeconds(null,{day:7});
        beginSeconds:function  (date,opt) {
            var d ,day,year,month;
            if (!date) {
                date = new Date();
            }

            if (!opt) {//\u5982\u679c\u6ca1\u6709\u53c2\u6570\u5219\u662f\u6628\u5929\u665a\u4e0a\u6700\u540e\u4e00\u65f6
                date = new Date();
                date.setHours(0);
                date.setMinutes(0);
                date.setSeconds(0)
                _day = 0
                _year = 0
                _month = 0
            }
            else{
                _day = !isNaN(opt.day) ? opt.day : 0;
                _year = !isNaN(opt.year) ? opt.year : 0;
                _month = !isNaN(opt.month) ? opt.month : 0;

            }
            d =  new Date(date.getTime());
            var time = Date.UTC(
                    d.getFullYear()-_year,
                    d.getMonth()-_month,
                    d.getDate()-_day,
                    d.getHours()-8,
                d.getMinutes(),
                d.getSeconds()
            );
            //var s = Highcharts.dateFormat("%Y-%m-%d %H:%M:%S",time );
            //console.log(s);
            return parseInt(time/1000)
        },
        dateFormat:function  (d,fmt) {
            var o = {
                "M+": d.getMonth() + 1, //\u6708\u4efd 
                "d+": d.getDate(), //\u65e5 
                "h+": d.getHours(), //\u5c0f\u65f6 
                "m+": d.getMinutes(), //\u5206 
                "s+": d.getSeconds(), //\u79d2 
                "q+": Math.floor((d.getMonth() + 3) / 3), //\u5b63\u5ea6 
                "S": d.getMilliseconds() //\u6beb\u79d2 
            };
            if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (d.getFullYear() + "").substr(4 - RegExp.$1.length));
            for (var k in o)
                if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
            return fmt;
        },
        getUTCANormalDiff :function(){
            var now = new Date();
            var nyear = now.getUTCFullYear();
            var nmonth = now.getUTCMonth();
            var ndate = now.getUTCDate();
            var nhour = now.getUTCHours();
            var nminute = now.getUTCMinutes();
            var nsecond = now.getUTCSeconds();
            //\u83b7\u53d6UTC\u7684\u6beb\u79d2\u6570
            var UTCMiniSecond = new Date(nyear,nmonth,ndate,nhour,nminute,nsecond).getTime();
            //\u83b7\u53d6\u5f53\u524d\u65f6\u95f4\u548cUTC\u65f6\u95f4\u7684\u65f6\u95f4\u5dee
            var diffMiniSecond = now.getTime() - UTCMiniSecond;
            var diffHour = Math.round(diffMiniSecond/(1000*60*60));
            return diffHour;
        },
        getUTCHours: function(h){
            var diffUTC = tool.getUTCANormalDiff();
            h = h - diffUTC;
            if(h<0){
                return 24 + h;
            }else{
                return h;
            }
        },
        serialiseObject: function(queryObj) {
            var pairs = [];
            for (var prop in queryObj) {
                if (!queryObj.hasOwnProperty(prop)) {
                    continue;
                }
                pairs.push(prop + '=' + queryObj[prop]);
            }

            // pairs.push("t=" + new Date().getTime());
            return pairs.join('&')
        },
        hideToast: function(cls) {
            var t = $('.toast-ct' + '.' + cls);
            if (t) {
                t.remove();
            };
        },
        //params:{cls:'cls',message:'message',duration:1000,hold:true}
        //\u5982\u679c{hold:true,cls:'cls'}\u9700\u8981\u624b\u52a8\u8c03\u7528 hideToast('cls')
        toast: function(params) {
            //if (!params.cls) {return};
            var cls = params.cls || 'cls';
            var _this = this;
            var toast, toastInner;
            var pcls = '.' + cls;
            var count = $('.toast-ct').length;
            toast = $('.toast-ct' + pcls);
            if (!toast.length) {
                $(document.body).append('<div class="toast-ct ' + cls + '"><div class="toast"></div></div>');
                toast = $('.toast-ct' + pcls);
            }


            toastInner = toast.find('.toast');
            toast = toastInner.parent();
            toastInner.html(params.message);

            if (!toast.hasClass('show')) {
                toast.addClass('show');
                var height = toast.height();
                var bottom = toast.css('bottom').replace('px', '');
                bottom = parseInt(bottom) + (height * count) + (count * 10);
                toast.css('bottom', (bottom) + 'px')

                if (!params.hold) {
                    (function(cls, duration) {
                        setTimeout(function() {
                            tool.hideToast(cls);
                        }, duration || 5000);
                    })(cls, params.duration);
                }
            }

        },
        tip: function(str) {
            tool.toast({
                message: str,
                cls: "cls",
                duration: 3000
            });
        },
        loadshow: function(str, timeout) {
            var loadEl = this.loadEl || (this.loadEl = document.querySelector('.ui-load'));
            if(!loadEl){
                $("body").append('<div class="ui-load"><div class="ui-loadtext"><i class="iconfont">&#xe625;</i><span id="J_loadtxt"></span></div></div>');
                loadEl = this.loadEl = document.querySelector('.ui-load')
            }
            if (str) {
                loadEl.querySelector('#J_loadtxt').innerHTML = str;
            }
            loadEl.style.display = 'block';
            if(timeout){
                setTimeout(function(){
                    loadEl.style.display = 'none';
                }, timeout);
            }
        },
        loadhide: function() {
            var loadEl = this.loadEl || (this.loadEl = document.querySelector('.ui-load'));
            if (!loadEl) {
                return;
            }
            loadEl.style.display = 'none';
        },
        bindEvents: function(bindings,unbind) {
            if (unbind===true) {this.unbindEvents(bindings)};
            for (var i in bindings) {
                if(bindings[i].selector) {
                    $(bindings[i].element)
                        .on(bindings[i].event,bindings[i].selector , bindings[i].handler);
                }else{
                    $(bindings[i].element)
                        .on(bindings[i].event, bindings[i].handler);
                }
            }
        },
        unbindEvents: function(bindings) {
            for (var i in bindings) {
                if(bindings[i].selector) {
                    $(bindings[i].element)
                        .off(bindings[i].event,bindings[i].selector , bindings[i].handler);
                }else{
                    $(bindings[i].element)
                        .off(bindings[i].event, bindings[i].handler);
                }
            }
        },
        map:function  () {
            var size = 0; //Map\u5927\u5c0f
            var entry = new Object();
            var map = {
                //\u5b58
                put: function(key, value) {
                    if (!this.containsKey(key)) {
                        size++;
                    }
                    entry[key] = value;
                },

                //\u53d6
                get: function(key) {
                    if (this.containsKey(key)) {
                        return entry[key];
                    } else {
                        return null;
                    }
                },

                //\u5220\u9664
                remove: function(key) {
                    if (delete entry[key]) {
                        size--;
                    }
                },

                //\u662f\u5426\u5305\u542b Key
                containsKey: function(key) {
                    return (key in entry);
                },

                //\u662f\u5426\u5305\u542b Value
                containsValue: function(value) {
                    for (var prop in entry) {
                        if (entry[prop] == value) {
                            return true;
                        }
                    }
                    return false;
                },

                //\u6240\u6709 Value
                values: function() {
                    var values = new Array(size);
                    for (var prop in entry) {
                        values.push(entry[prop]);
                    }
                    return values;
                },

                //\u6240\u6709 Key
                keys: function() {
                    var keys = new Array(size);
                    for (var prop in entry) {
                        keys.push(prop);
                    }
                    return keys;
                },

                //Map Size
                size: function() {
                    return size;
                }
            }
            return map;
        },
        showNotNetworkTip:function  (top) {
            top = top || '0';
            var s = '<div class="not-network" style="top:'+top+'"><div class="network"><h1>\u8be5\u8bbe\u5907\u5df2\u65ad\u5f00\u7f51\u7edc</h1><p>\u8bf7\u5c06\u8bbe\u5907\u8fde\u4e0a\u60a8\u7684\u7f51\u7edc\uff0c\u60a8\u53ef\u4ee5\u5c1d\u8bd5\u4ee5\u4e0b\u64cd\u4f5c\uff1a</p><ul><li>\u68c0\u67e5\u667a\u80fd\u8bbe\u5907\u7684\u7535\u6e90\u662f\u5426\u63d2\u597d\u3002</li><li>\u68c0\u67e5\u5bb6\u91cc\u7684\u8def\u7531\u5668\u662f\u5426\u6210\u529f\u8fde\u7f51\uff0c\u6216\u5c1d\u8bd5\u91cd\u542f\u8def\u7531\u5668\u3002</li></ul></div></div>'
            var el = $(s);
            var html = $('html')[0]
            this.showNotNetworkTip._style_overflow = html.style.overflow;
            html.style.overflow = 'hidden';
            $(html).append(el);
        },
        hideNotNetworkTip:function  () {
            $('html')[0].style.overflow = this.showNotNetworkTip._style_overflow;
            $('.not-network').remove()
        },
        openTimer:function  (opt) {
            opt = opt || {};
            opt.__d = '20150805';
            DA.loadPage('http://g.alicdn.com/aic/sdk/extra-components/timeCase/index.html',_.extend({
                fromUrl:location.href.replace('#','')
            },opt))
        },
        obj_to_string: function (obj) {
            return dump_to_string(obj);
        }
    };
    DA.toast = tool.toast;
    DA.hideToast = tool.hideToast;
    DA.tip = tool.tip;
    DA.urlParam = tool.urlParam;
    DA.getUTCHours = tool.getUTCHours;
    DA.bindEvents = tool.bindEvents;
    DA.unbindEvents = tool.unbindEvents;
    DA.map = tool.map();

    DA.loadshow = tool.loadshow;
    DA.loadhide = tool.loadhide;
    DA.modal  = tool.modal;
    DA.closeModal = tool.closeModal;
    DA.alert = tool.alert;
    DA.openTimer = tool.openTimer;
    DA.appTool = tool;
    DA.obj_to_string = tool.obj_to_string;
    return tool;
});
/**
 * fangyuan.yzh
 * \u7528\u4e8e\u524d\u7aef\u53d1\u6d88\u606f\u5230native sdk
 */
define('_sdk_api',['windvane','_sdk_tool'], function(wv, tool) {

    var LOOP = function(){};
    var sdk = {
        wsfLoginCalled: 0,
        request: {
            "token": "", //\u7531native \u8865\u5168
            "host": "com.aliyun.alink",
            "hostType": "app",
            "version": "1.0.0", //\u7531native \u8865\u5168
            "target": "",
            "account": ""
        },
        repCode: {
            //\u7528\u6237\u5fc5\u987b\u8f93\u5165\u8d26\u53f7\u5bc6\u7801\u767b\u5f55
            LOGIN_TOKEN_ILLEGAL: {
                '3084': 1
            },
            APP_NOT_LOGIN: {
                '3002': 1,
                'INVOKE_LOGIN_ERROR': 1
            },
            SUCCESS: {
                '1000': 1
            },
            INVOKE_NET_ERROR: {
                'INVOKE_NET_ERROR': 1
            },
            INVOKE_SERVER_ERROR: {
                'INVOKE_SERVER_ERROR': 1
            }
        },
        _afterCallbacks: [],
        //opt: \u53c2\u6570\uff0ccb \uff1a\u6210\u529f\u540e\u6bc1\u6389\u51fd\u6570 fcb:\u5931\u8d25\u540e\u56de\u8c03\u51fd\u6570
        __alinkRequestWsfProxy: function(opt, cb, fcb) {
            console.log('alinkRequestWsfProxy:', opt);
            //\u5982\u679cwsf\u672a\u767b\u5f55 \u5219\u5148\u767b\u9646
            if (sdk.wsfLoginCalled == 0 || sdk.wsfLoginCalled == 1) {
                var args = arguments;
                var fn = (function(args) {
                    return function() {
                        sdk.alinkRequestWsfProxy.apply(sdk, args);
                    }
                })(args);
                sdk._afterCallbacks.push(fn);
                if (sdk.wsfLoginCalled == 0) {
                    sdk.wsfLoginCalled = 1;
                    sdk.loginUser({}, function(data) {
                        sdk.wsfLoginCalled = 2;
                        sdk.alinkRequestWsfProxy = sdk._alinkRequestWsfProxy;
                        var code = data.code || data.result.code;
                        if (sdk.repCode.LOGIN_TOKEN_ILLEGAL[code]) {
                            DA.loginTip();
                        }


                        for (var i = sdk._afterCallbacks.length - 1; i >= 0; i--) {
                            sdk._afterCallbacks[i]();
                        };



                    });
                }

                return;
            };
        },
        alinkRequestWsfProxy: function(opt, cb, fcb, request) {
            opt = opt || {};
            cb = cb || function() {};
            fcb = fcb || function() {};
            opt.request = request || sdk.request;
            this.wv.call('AlinkRequest', 'wsfProxy', opt, cb, fcb);
        },
        alinkBleRequestWsfProxy: function(opt, cb, fcb, request) {
            opt = opt || {};
            cb = cb || function() {};
            fcb = fcb || function() {};
            opt.request = request || sdk.request;
            this.wv.call('AlinkRequest', 'bleProxy', opt, cb, fcb);
        },
        
        wv: {
            /**
             * \u8c03\u7528windvane
             * @param  {String} ctrl   native\u7c7b
             * @param  {String} method \u7c7b\u7684\u65b9\u6cd5
             * @param  {Object} params JSON\u53c2\u6570
             * @return {Undefined}
             */
            call: function(ctrl, method, params, cb, fcb) {
                

                var me = sdk;
                window.WindVane && window.WindVane.call(
                    ctrl,
                    method,
                    params || {},
                    function(data) {
                        console.log('WSF\u63a5\u53e3\u8c03\u7528: ', params.method, "\u5165\u53c2", params, " & \u8fd4\u56de\u6570\u636e: ", data);
                        var code = data.code || (data.result && data.result.code) || '';
                        if (sdk.repCode.LOGIN_TOKEN_ILLEGAL[code] && !DA.query.testmode) {
                            DA.loginTip();
                            return;
                        }
                        cb && cb(data);
                    },
                    fcb
                );
            }

        },
        
        // \u5224\u65ad\u662f\u5426\u5728\u5ba2\u6237\u7aef\u73af\u5883\u4e2d
        isInApp: (function() {
            return navigator.userAgent.indexOf('WindVane') != "-1";
        })(),
        pushWebView: function(params, cb, fcb) {
            // params.url = 'http://www.taobao.com';
            if (sdk.isInApp) {
                sdk.wv.call('AlinkHybrid', 'pushWebView', params, function(){
                    console.log(arguments);
                }, function(){
                    console.log(arguments);
                });
            } else {
                window.location.href = params.url;
            }
        },
        //pop webview 
        popWebView: function(params, cb, fcb) {
            params = params || {};
            if (this.isInApp) {
                this.wv.call('AlinkHybrid', 'popWebView', params, cb, fcb);
            } else if(params.url){
                window.location.href = params.url;
            }else{
                window.history.back();
            }
        },
    };
    
    var wsfMap = [
        'loginUser',  //\u8fc7\u6ee4
        'getCurrentAccountInfo',    //\u8fc7\u6ee4
        'getSubDevicesByGateway',  
        'queryCaseSnapshot',
        'getDeviceProperty',   //\u8fc7\u6ee4
        'setDeviceProperty',   //\u8fc7\u6ee4
        'getDeviceStatusHistory',  
        'getDeviceDataHistory',   //?
        'reassignDevData',    //\u8fc7\u6ee4
        'checkProvisionSupporting',   //\u8fc7\u6ee4
        'startDeviceProvision',     //\u8fc7\u6ee4
        'stopDeviceProvision',     //\u8fc7\u6ee4
        'stopDiscover',     //\u8fc7\u6ee4
        'startDiscover',     //\u8fc7\u6ee4
        'requestRouterNameInfo',   //\u8fc7\u6ee4
        'requestRouterUUID',      //\u8fc7\u6ee4
        'discoverLocalDevices',     //\u8fc7\u6ee4
        'registerDeviceByUser',     //\u8fc7\u6ee4
        'bindDeviceByUser',     //\u8fc7\u6ee4
        'unbindDeviceByUser',     //\u8fc7\u6ee4
        'dismissUserFromDevice',   //\u8fc7\u6ee4
        'setDeviceStatus', 
        'postDeviceDataArray', //?
        'getDeviceStatusHistory', //11111111111\u91cd\u590d
        'getOutdoorWeather', 
        'updateDeviceInfo',   //\u8fc7\u6ee4
        'getDeviceStatus',
        'getDeviceStatusArray', //\u8fc7\u6ee4
        'getDeviceInfo',
        'getAlinkTime', //?
        'getDevicesByUser',  //\u8fc7\u6ee4
        'getUsersByDevice',  //\u8fc7\u6ee4
        'getModelInfo',  //\u8fc7\u6ee4
        'requestWifiLevel',  //\u8fc7\u6ee4
        'getUserFriend',  //\u8fc7\u6ee4
        'createUserFriend',  //\u8fc7\u6ee4
        'deleteUserFriend',  //\u8fc7\u6ee4
        'getUserDataArray',  //\u8fc7\u6ee4
        'postUserDataArray',  //\u8fc7\u6ee4
        'getUserDataHistory',  //\u8fc7\u6ee4
        'backupAppData',
        'retrieveAppData',
        'getCameraLiveStreamingURL',   //\u8fc7\u6ee4
        'msgcenter_getAlarmHistory',
        'msgcenter_updateAlarmStatus',
        'setDeviceStatusArray', 
        'appstore_getAppAvailableTag'
        ,'appstore_getAppControlInstructions'
        ,'appstore_getDeviceApp'
        ,'appstore_getAppDetailedInfo'
        ,'appstore_getUserDevApp'
        ,'appstore_markFavoriteApp'
        ,'appstore_unMarkFavoriteApp'
        ,'appstore_getAppUpdateStatus'
        ,'appstore_runApp'
        ,'appstore_getUserContext'
        ,'appstore_updateUserContext',
        'appstore_getAppAdjustableAttribute',
        'case_queryCaseSnapshot', //method: 'case/queryCaseSnapshot'
        'case_queryCase',  
        'case_queryCaseTemplateList',  
        'case_queryCaseList', 
        'case_updateCaseState',  
        'case_updateCase',  
        'case_addCase',  
        'case_removeCase', 
        'case_runCase', 
        'case_stopCase', 
        'case_queryDeviceRunSnapshot',  
        'case_queryDeviceList',
        'case_addTemplateCase',
        'case_updateTemplateCase',
        'case_queryTemplateCase',
        'case_queryTemplateCaseList',
        'case_getAdvicePlan',
        'case_updatePM25Value',
        'case_queryDeviceSchedule',
        'case_feedbackAdvicePlan',
        'dt.getDynamicData',
        'dt.calculateAlgorithm',
        'dt.getDynamicData'
    ];
    var bleAPIs = [
    'initialize',
    'isInitialized',
    'close',
    'isEnabled',
    'isConnected',
    'connect',
    'reconnect',
    'discover',
    'characteristics',
    'disconnect',
    'startScan',
    'stopScan',
    'isScanning',
    'read',
    'write',
    'subscribe',
    'unsubscribe',
    'readDescriptor',
    'writeDescriptor',
    'descriptors',
    'rssi',
    'services'
    ];
    
    DA.ble = DA.ble ? DA.ble :{};
    bleAPIs.forEach(function  (method) {
        DA.ble[method] =function  (params, cb, fcb, request) {
            var opt = {
                method: method,
                params: params
            };
            sdk.alinkBleRequestWsfProxy(opt, cb, fcb, request);
        }
    });
    wsfMap.forEach(function(item){
        DA[item] = function  (params, cb, fcb, request) {
            var method = item.replace('_', '/');
            var opt = {
                method: method,
                params: params
            };
            sdk.alinkRequestWsfProxy(opt, cb, fcb, request);
        }
    });

    var hybirdMap = [
        'toLogin',  //\u8fc7\u6ee4
        'logout',  //\u8fc7\u6ee4
        'switchH5RootUrl',  //\u8fc7\u6ee4
        'pushWebView',
        'popWebView',

        'toggleSwipeBack',
        'WVAlinkUserTrack'
    ];
    hybirdMap.forEach(function(item){
        DA[item] = function  (params, cb, fcb) {
            sdk.wv.call('AlinkHybrid', item, params || {}, cb, fcb);
        }
    });
    var wvlocationMap = [
        'getLocation',
        'searchLocation'
    ];
    wvlocationMap.forEach(function(item){
        DA[item] = function  (params, cb, fcb) {
            sdk.wv.call('WVLocation', item, params || {}, cb, fcb);
        }
    });
    DA.hasNetWork = function(cb){
        sdk.wv.call('WVNetwork', 'getNetworkType', {}, function(data){
            cb(data.type !== 'NONE');
        })
    }
    DA.playSystemSound = function(params, cb, fcb){
        sdk.wv.call('WVAudio', 'playSystemSound', params || {}, cb, fcb);
    }
    DA.backupDeviceData = function(uuid, params, cb, fcb, request){
        var opt = {
            method: 'backupDeviceData',
            params: params
        };
        var request = sdk.request;
        request.uuid = uuid;
        sdk.alinkRequestWsfProxy(opt, cb, fcb, request);
    }
    DA.retrieveDeviceData = function(uuid, params, cb, fcb, request){
        var opt = {
            method: 'retrieveDeviceData',
            params: params
        };
        var request = sdk.request;
        request.uuid = uuid;
        sdk.alinkRequestWsfProxy(opt, cb, fcb, request);
    }
    DA.getOutdoorWeather = function (params, cb, fcb) {
        var opt = {
            method: 'service.weather.query',
            params: params
        };
        sdk.alinkRequestWsfProxy(opt, cb, fcb);
    }
    DA.getRemoteContent = function(url, cb, fcb){
        sdk.wv.call('AlinkRequest', 'doGetRemoteContent', {
            url: url
        }, cb, fcb);
    }
    DA.scanURL = function(cb, fcb){
        sdk.wv.call('AlinkRequest', 'scanURL', '', cb, fcb);
    }

    DA.recordUT = function(eventName, eventArgs, cb, fcb){
		// @UT\u57cb\u70b9 http://gitlab.alibaba-inc.com/alinkapp/android-alinkapp/issues/2
		sdk.wv.call('AlinkUserTrack', 'commitEvent', {'eventID': eventName, 'properties': eventArgs}, cb || function() {}, fcb || function() {});		
    }
	
    DA.nativeStorage = {
        setStorage: function(params, cb, fcb) {
            var opt = {
                method: 'setItem',
                params: params
            };
            sdk.wv.call('AlinkHybrid', 'storage', opt, cb || function() {}, fcb || function() {});
        },
        getStorage: function(params, cb, fcb) {
            var opt = {
                method: 'getItem',
                params: params
            };
            sdk.wv.call('AlinkHybrid', 'storage', opt, cb || function() {}, fcb || function() {});
        },
        removeStorage: function(params, cb, fcb) {
            var opt = {
                method: 'removeItem',
                params: params
            };
            sdk.wv.call('AlinkHybrid', 'storage', opt, cb || function() {}, fcb || function() {});
        },
    }

    DA.getSubDeviceInfo = function(param, callback) {
        var muuid = param.muuid, suuid = param.suuid;
        DA.getDeviceInfo({
            uuid: suuid
        }, function(info) {
            var result = info.result || {};
            if (result.msg == 'success') {
                console.log('getSubDeviceInfoWithCallback', JSON.stringify(info.result.data));
                callback && callback(info.result.data);
            }
        }, function() {

        }, {
            "token": "", //\u7531native \u8865\u5168
            "host": "com.aliyun.alink",
            "hostType": "app",
            "version": "1.0.0", //\u7531native \u8865\u5168
            "target": muuid,
            "account": ""
        });
    }
    setTimeout(function(){
        //\u9ed8\u8ba4\u5f00\u542f\u5de6\u6ed1
        DA.toggleSwipeBack({enable:'1'}) 
    }, 1000);


    window.alert = DA.alert = function(msg){
        var params = {
            // \u8b66\u544a\u6846\u8981\u663e\u793a\u7684\u6d88\u606f
            message: msg.toString(),
            // \u8b66\u544a\u6846\u7684\u786e\u8ba4\u6309\u94ae\u6587\u672c
            okbutton: '\u786e \u5b9a'
        };
        window.WindVane.call('WVUIDialog', 'alert', params, function(e) {
        }, function(e) {
        });
    };
    var confirmOkCallback;
    var confirmCancelCallback;
    var confirmParams;
    document.addEventListener('wv.dialog', function(e) {
        if(e.param.type == confirmParams.okbutton){
            confirmOkCallback && confirmOkCallback();
        }else if(e.param.type == confirmParams.canclebutton){
            confirmCancelCallback && confirmCancelCallback();
        }
    }, false);
    window.confirm = DA.confirm = function(opt, okcallback, cancelcallback){
        confirmOkCallback = okcallback;
        confirmCancelCallback = cancelcallback;
        confirmParams = {
            // \u786e\u8ba4\u6846\u8981\u663e\u793a\u7684\u6d88\u606f
            message: opt.msg.toString(),
            // \u786e\u8ba4\u6846\u7684\u786e\u8ba4\u6309\u94ae\u6587\u672c
            okbutton: opt.ok || '\u786e \u5b9a',
            // \u786e\u8ba4\u6846\u7684\u53d6\u6d88\u6309\u94ae\u6587\u672c
            canclebutton: opt.cancel || '\u53d6 \u6d88',
            // \u786e\u8ba4\u6846\u7684\u7d22\u5f15
            _index: 10087
        };
        window.WindVane.call('WVUIDialog', 'confirm', confirmParams, function(e) {
            // console.log(e);
            // if(e.type == confirmParams.okbutton){
            //     confirmOkCallback && confirmOkCallback();
            // }else if(e.type == confirmParams.canclebutton){
            //     confirmOkCallback && confirmOkCallback();
            // }
        }, function(e) {
            // alert('failure: ' + JSON.stringify(e));
        });
        return false;
    }
    return sdk;
});
define('_sdk_storage',[],function() {
    
    function TinyStore(name) {
        
        this.enabled = (function() {
            try {
                return 'localStorage' in window && window['localStorage'] !== null;
            } catch (e) {
                return false;
            }
        })();

        this.session = {};

        if (this.enabled) {
            try {
                this.session = JSON.parse(localStorage.getItem(name)) || {};
            } catch (e) {}
        }

        this.save = function() {
            if (this.enabled) {
                localStorage.setItem(name, JSON.stringify(this.session));
            }
            return this.session;
        };

        this.set = function(key, value) {
            this.session[key] = value;
            this.save();
            return this.session[key];
        };

        this.get = function(key) {
            return this.session[key];
        };

        this.remove = function(key) {
            delete this.session[key];
            return this.save();
        };

        this.clear = function() {
            this.session = {};
            return this.save();
        };
    }
    
    return TinyStore;

})
;
define('_sdk_alink',['_sdk_tool', '_sdk_api', '_sdk_storage'], function(tool, api, Storage) {

	var requestOpt = {
		"token": "", //\u7531native \u8865\u5168
		"host": "com.aliyun.alink",
		"hostType": "app",
		"version": "1.0.0", //\u7531native \u8865\u5168
		"target": "",
		"account": ""

	};
	var logqueue = [];

	var localStorageEnv = new Storage('env');
	var sceneMap = {};
	var _put = function(name, value) {
		sceneMap[name] = value;
	}
	var _get = function(name) {
		return sceneMap[name];
	}


	var alink = {
		loginTip: function() {
			tool.toast({
				message: '\u4eb2!\u60a8\u9700\u8981\u767b\u5f55\u540e\u624d\u80fd\u8fdb\u884c\u64cd\u4f5c,<span class="blue">\u70b9\u51fb\u767b\u5f55</span>',
				cls: 'J-toLogin',
				hold: true
			})
			window._GLOBAL_NEED_LOGIN = true;
		},
		notNetworkTip: function() {
			tool.toast({
				message: '\u4eb2! \u5f53\u524d\u7f51\u7edc\u4e0d\u53ef\u7528,\u8bf7\u68c0\u67e5\u60a8\u7684\u7f51\u7edc\u8bbe\u7f6e',
				cls: 'network-tip-down',
				hold: true
			});
		},
		// \u7ed1\u5b9a\u6570\u636e\u4e8b\u4ef6\u76d1\u542c\u63a5\u53e3
		bindPushData: function(objs) {
			_.each(objs, function(item, key) {
				if (_.isFunction(item)) {
					_put(key, item);
				}
			})
		},
		// \u5411APP\u53d1\u9001\u6570\u636e
		sendApp: function(method, data) {
			var func = _get(method);
			alink.log(method, JSON.stringify(data));
			func && func(data);
		},
		getDeviceStatus: function(uuid, callback, request) {
			var self = this;
			var attrSet = [];
			request = request || {};
			request = _.defaults(request, requestOpt);
			var _uuid = uuid || DA.uuid;
			var data = {
				uuid: _uuid,
				attrSet: []
			};

			var opt = {
				method: 'getDeviceStatus',
				params: data
			};
			api.alinkRequestWsfProxy(opt, function(d) {

				try {
					if (d.code == 'INVOKE_NET_ERROR') {
						// DA.loadshow('E1:' + d.msg);
						alink.error('E1: \u5f53\u524d<span class="red">\u7f51\u7edc\u4e0d\u4f73</span>\uff0c\u65e0\u6cd5\u64cd\u4f5c\uff0c\u8bf7\u7a0d\u5019\u518d\u8bd5\u6216\u8bbe\u7f6e\u60a8\u7684\u7f51\u7edc', d, 'getDeviceStatus');
						return;
					}
					// deviceError.html('');
					// deviceError.addClass('hidden');
					if (d.result && d.result.msg == 'success') {

						// \u5224\u65ad\u662f\u5426\u53ea\u6709onlineState\u5b57\u6bb5
						var result_data = d.result.data;
						var result_data_length = 0;
						var hasOnlineState = false;
						_.each(result_data, function(item, key) {
							if (key == 'onlineState') {
								hasOnlineState = true;
							} else if (key != 'uuid') {
								result_data_length++;
							}
						});

						if (hasOnlineState && result_data_length == 0) {
							alink.error('E2: \u5f88\u62b1\u6b49\uff0c\u60a8\u7684\u8bbe\u5907\u6570\u636e\u51fa\u9519\u4e86\uff0c<br><span class="red">\u65b0\u8bbe\u5907\u521d\u59cb\u5316\u4e2d\uff0c\u8bf71\u5206\u949f\u4ee5\u540e\u518d\u5c1d\u8bd5</span>\uff0c<br>\u5982\u4ecd\u6709\u95ee\u9898\u8bf7\u8054\u7cfb\u670d\u52a1\u70ed\u7ebf\uff01', d, 'getDeviceStatus');
							return;
						}

						alink.log('\u83b7\u53d6\u8bbe\u5907\u6570\u636e--getDeviceStatus\uff1a', JSON.stringify(d.result.data));
						callback && callback(d.result.data);
					} else if (d.result && d.result.msg === 'no bind relation') {
						// DA.loadshow('');
						alink.error('E3: \u5f88\u62b1\u6b49\uff0c\u60a8\u7684\u8bbe\u5907\u5df2\u7ecf\u88ab\u89e3\u7ed1\uff0c\u8bf7\u60a8<span class="red">\u91cd\u65b0\u7ed1\u5b9a</span>', d, 'getDeviceStatus');
						return;
						// alinkSDK.bindDeviceByUserWithUuid({uuid: uuid}, function(){
						//     self.getData(callback);
						// });
					} else if (d.result && d.result.code == 3084) {
						alink.error('E3: \u5f88\u62b1\u6b49\uff0c\u60a8\u7684\u8bbe\u5907\u5df2\u7ecf\u88ab\u89e3\u7ed1\uff0c\u8bf7\u60a8<span class="red">\u91cd\u65b0\u7ed1\u5b9a</span>', d, 'getDeviceStatus');
						return;
					} else {
						// bugfix:APP\u8fd0\u884c\u671f\u95f4\uff0c\u51fa\u73b0\u5f02\u5e38\u63d0\u793a\uff1aloadhide\u4e0d\u662f\u4e00\u4e2a\u51fd\u6570
						if (typeof tool.loadhide == "function") {
							tool.loadhide();
						}

						// tool.tip('\u8bbe\u5907\u72b6\u6001\u540c\u6b65\u5931\u8d25');
						alink.error('E4: \u5f53\u524d<span class="red">\u7f51\u7edc\u4e0d\u4f73</span>\uff0c\u65e0\u6cd5\u64cd\u4f5c\uff0c\u8bf7\u7a0d\u5019\u518d\u8bd5\u6216\u8bbe\u7f6e\u60a8\u7684\u7f51\u7edc', d, 'getDeviceStatus');
						return;
					}


				} catch (e) {

					var t = new Date();
					var month = t.getMonth() + 1;
					var tt = t.getFullYear() + '-' + month + '-' + t.getDate() + ' ' + t.getHours() + ':' + t.getMinutes() + ':' + t.getSeconds();
					var env = alink.getEnv().env;
					if (env == "dev" || env == "test" || env == "awpcdn" || env == "jssdk") {
						alink.error('\u5f88\u62b1\u6b49\uff0c\u60a8\u7684\u8bbe\u5907<span class="red">' + DA.uuid + '</span>\u51fa\u73b0\u672a\u77e5\u5f02\u5e38, <br>\u53d1\u751f\u65f6\u95f4\uff1a' + tt + '\uff0c<br>\u884c\u6570\uff1a<span class="red">' + e.line + '</span><br>\u9519\u8bef\u4fe1\u606f\uff1a<span class="red">' + e.message + '</span><br>\u6587\u4ef6\uff1a' + e.sourceURL, d, 'getDeviceStatus');
					} else {
						alink.error('\u5f88\u62b1\u6b49\uff0c\u60a8\u7684\u8bbe\u5907<span class="red">' + DA.uuid + '</span>\u51fa\u73b0\u672a\u77e5\u5f02\u5e38, <br>\u53d1\u751f\u65f6\u95f4\uff1a' + tt + '\uff0c<br>\u8bf7\u60a8\u91cd\u542f\u60a8\u7684\u8bbe\u5907\u6216\u8005\u91cd\u65b0\u6253\u5f00\u963f\u91cc\u5c0f\u667a\uff0c\u5982\u95ee\u9898\u672a\u89e3\u51b3\uff0c\u8bf7\u8054\u7cfb\u6211\u4eec\u7684\u5ba2\u670d\u70ed\u7ebf\uff01', d, 'getDeviceStatus');
					}
				}

			}, function(d) {
				alink.error('E5: \u5f53\u524d\u7f51\u7edc\u4e0d\u4f73\uff0c\u65e0\u6cd5\u64cd\u4f5c\uff0c\u8bf7\u7a0d\u5019\u518d\u8bd5\u6216\u8bbe\u7f6e\u60a8\u7684\u7f51\u7edc', d, 'getDeviceStatus');
			}, request);
		},
		setDeviceStatus: function(uuid, data, callback, request) {
			var attrSet = [];
			request = request || {};
			uuid = uuid || DA.uuid;
			if (!uuid) {
				console.error('uuid\u4e0d\u5b58\u5728');
				return;
			}
			request = _.defaults(request, requestOpt);
			for (var key in data) {
				attrSet.push(key);
			}
			data['attrSet'] = attrSet;
			data.uuid = uuid;
			var opt = {
				method: 'setDeviceStatus',
				params: data
			};

			alink.log('\u6307\u4ee4\u4e0b\u53d1 - setDeviceStatus:', JSON.stringify(data));
			api.alinkRequestWsfProxy(opt, function(res) {
				if (res.result) {
					if (res.result.msg == 'success') {
						callback && callback(true, res);
						return;
					}
				}
				tool.loadshow('\u6307\u4ee4\u4e0b\u53d1\u5931\u8d25', 3000);
			}, function(res) {
				tool.loadshow('\u6307\u4ee4\u4e0b\u53d1\u5931\u8d25', 3000);
				callback && callback(false, res);
			}, request);
		},
		setSubDeviceStatus: function(uuids, param, cb) {
			var mainuuid = uuids.muuid;
			var subuuid = uuids.suuid;
			alink.setDeviceStatus(subuuid, param, cb, {
				target: mainuuid
			});
		},
		getSubDeviceStatus: function(uuids, cb) {
			var mainuuid = uuids.muuid;
			var subuuid = uuids.suuid;
			alink.getDeviceStatus(subuuid, cb, {
				target: mainuuid
			});
		},
		error: function(msg, d, method) {
			console.error('\u9519\u8bef--' + method + '\uff1a', JSON.stringify(d));

			if (msg) {
				// DA.disabledDashboard();

				var deviceData = '';
				var localEnv = alink.getEnv();
				var env = localEnv.env;
				if (env == "dev" || env == "test" || env == "awpcdn" || env == "jssdk") {
					if (d) {
						deviceData = '<div style="height:150px;width:100%;overflow:scroll;background: #000; padding: 3px; color:#fff;"><div>' + JSON.stringify(d) + '</div></div>';
					}
				}
				var html = '<section class="panel-control panel-error device-error J_device_error"><p class="p1">\u5f02\u5e38\u63d0\u793a</p><p class="p2">' + msg + '</p>' + deviceData + '<p class="p3"><img src="http://gtms03.alicdn.com/tps/i3/TB1bDijHpXXXXbOXFXXjumS_VXX-237-239.png" height="100"></p></section>';

				$("body").append(html);
			}
			var data = {
				method: method,
				m: DA.modelName,
				u: DA.uuid,
				n: DA.nickName,
				a: DA.userId,
				msg: d,
				env: localEnv,
				log: msg,
				platform: ''
			}
			window.WindVane.call('AlinkRequest', 'getEnvStatus', {}, function(wsf) {
				if (wsf && wsf['tcpAddr']) {
					data.wsf = wsf['tcpAddr'];
				} else {
					data.wsf = 'online';
				}
				alink.sendLog(data);
			}, function(wsf) {
				data.wsf = 'online';
				alink.sendLog(data);
			});
		},
		sendLog: function(data) {
			var log = tool.serialiseObject(data);
			var img = new Image();
			img.src = 'http://open.alink.aliyun.com/openapi/errorlog.php?' + log;
		},
		log: function(type, log) {
			var d = new Date();
			log = '\u3010' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds() + '\u3011' + '\u3010' + type + '\u3011' + log;
			console.debug(log);
			var core = function() {
				var wslog = {
					"type": "send",
					"to_client_id": DA.nickName + "::" + DA.uuid,
					"model": DA.modelName,
					"uuid": DA.uuid,
					"deviceName": DA.deviceName,
					"nick": DA.nickName,
					"uid": DA.userId,
					"content": log
				};
				if (typeof ws != "undefined") {

					if (ws.isReady) {
						ws.send(JSON.stringify(wslog));
					}else{
						logqueue.push(wslog);
					}
				}else{
					logqueue.push(wslog);
				}

			}
			if (type == 'getDeviceStatus') {
				setTimeout(core, 2000);
			} else {
				core();
			}


		},
		setEnv: function(param) {
			if (param.env) {
				localStorageEnv.set('env', {
					env: param.env, //test jssdk omn awpcdn
					tag: param.tag,
					time: param.time || '',
					name: decodeURI(param.name) || '\u672a\u77e5'
				});
			} else {
				var url = window.location.href;
				if (url.indexOf('m.taobao.com') > -1) {
					localStorageEnv.set('env', {
						env: 'awp',
						tag: '',
						time: '',
						name: ''
					});
				} else {
					localStorageEnv.set('env', {
						env: 'dev',
						tag: '',
						time: '',
						name: '\u5f00\u53d1'
					});
				}
			}
		},
		getEnv: function() {
			var env = localStorageEnv.get('env');
			// env.env = 'jssdk';
			if (!env) {
				return {
					'env': 'awp',
					'tag': '',
					'time': '',
					'name': ''
				}
			}
			return env;
		},
		loadPage: function(path, queryObj) {

			var targetAppUrl;
			if (path.indexOf('http://') == 0) {
				var _env = alink.getEnv(),
					env = _env.env;
				var queryParam = DA.query;
				queryObj = queryObj || {};
				queryObj.name = encodeURI(queryObj.name);
				queryObj = _.defaults(queryObj, _env);
				queryObj = _.defaults(queryObj, queryParam);
				var opt = {}
				for (var p in queryObj) {
					opt[encodeURIComponent(p)] = encodeURIComponent(queryObj[p])
				}
				var queryString = tool.serialiseObject(opt) || "";
				targetAppUrl = path + '?' + queryString;
			} else {
				targetAppUrl = alink.getWebUrl(path, queryObj);
			}
			if (!targetAppUrl) {
				return;
			}
			DA.nativeStorage.setStorage({
				itemKey: 'r:' + DA.modelName + ":" + path,
				itemValue: targetAppUrl
			});
			api.pushWebView({
				url: targetAppUrl
			})
		},
		_getWebUrlConfig: function(appName, queryObj){
			queryObj = queryObj ? queryObj : {};
			var _env = alink.getEnv(),
				env = _env.env;
			var queryParam = DA.query;
			queryObj = _.defaults(queryObj, _env);
			queryObj = _.defaults(queryObj, queryParam);
			var opt = {}
			for (var p in queryObj) {
				opt[encodeURIComponent(p)] = encodeURIComponent(queryObj[p])
			}
			var queryString = tool.serialiseObject(opt) || "";

			if ((appName[0] == '.' && appName[1] == '/')) {
				appName = appName.substring(2, appName.length);
			} else if (appName[0] == '/') {
				appName = appName.substring(1, appName.length);
			}
			return {
				appName : appName,
				queryString: queryString,
				queryParam: queryParam
			}
		},
		getWebUrl: function(appName, queryObj) {
			var currentUrl = "http://" + window.location.host,
				config = alink._getWebUrlConfig(appName, queryObj),
				appName = config.appName,
				queryString = config.queryString,
				queryParam = config.queryParam,
				targetAppUrl = '',
				_env = alink.getEnv(),
				env = _env.env;
			if (env == 'dev') {
				currentUrl = currentUrl + '/alink/device/' + DA.modelName;
				targetAppUrl = currentUrl + '/' + appName + "?" + queryString;
			}else if(env == 'jssdk'){
				currentUrl = currentUrl + '/' + DA.modelName;
				targetAppUrl = currentUrl + '/' + appName + "?" + queryString;
			} else if (env == 'test' || env == 'factory') {
				targetAppUrl = currentUrl + '/device/factory/' + queryParam.cdnpath + '/' + queryParam.dtag + '/' + appName + "?" + queryString;
			} else if (env == 'pre' || env == 'awpcdn') {
				targetAppUrl = currentUrl + '/device/pre/' + queryParam.cdnpath + '/' + queryParam.dtag + '/' + appName + "?" + queryString;
			} else if (env == 'wapp') {
				targetAppUrl = currentUrl + '/device/awp/' + queryParam.cdnpath + '/' + queryParam.dtag + '/' + appName + "?" + queryString;
			} else {
				targetAppUrl = currentUrl + '/device/appstore/' + queryParam.cdnpath + '/' + queryParam.dtag + '/' + appName + "?" + queryString;
			}
			return targetAppUrl;
		},
		back: function(appName, queryObj) {
			if (appName) {
				DA.nativeStorage.getStorage({itemKey: 'r:' + DA.modelName + ':' + appName}, function(d){
					var targetAppUrl = d['r:'+ DA.modelName + ":" + appName];
					if (targetAppUrl) {
						api.popWebView({
							url: targetAppUrl
						});
						return;
					}else{
						api.popWebView();
					}
				}, function(){
					api.popWebView();
				});
			}else{
				api.popWebView();
			}
		},
		scanJSSDK: function(queryObj, localEnv) {
			var self = this;
			var scan = function() {
				DA.scanURL(function(e) {
					var url = e.url,
						model = queryObj.model;
					if (url.indexOf(model) == -1) {
						setTimeout(function() {
							alert('URL\u4e0d\u5408\u6cd5\uff0c\u8def\u5f84\u5fc5\u987b\u5305\u542b' + model);
						}, 300);

						return;
					}
					go(url);
				});
			}
			var jssdkurlStorage = new Storage('jssdkurl');
			var go = function(url) {
				var param = _.defaults(queryObj, localEnv);
				var paramStr = tool.serialiseObject(param);
				if (url.indexOf('?') > 1) {
					url = url + '&' + paramStr;
				} else {
					url = url + '?' + paramStr;
				}
				jssdkurlStorage.set(queryObj.uuid + 'jssdkurl', {
					url: url,
					host: queryObj.host
				});
				DA.pushWebView({
					url: url
				});
			}

			var _url = jssdkurlStorage.get(queryObj.uuid + 'jssdkurl');
			if (_url && _url.url) {
				if (confirm('\u662f\u5426\u4f7f\u7528\u8be5\u5730\u5740')) {
					DA.pushWebView({
						url: _url.url
					});
				} else {
					scan();
				}
			} else {
				scan();
			}
		},
		istoDevice: false,
		pushDevice: function(queryObj) {
			if (alink.istoDevice) {
				return;
			}
			alink.istoDevice = true;
			queryObj = queryObj || {};
			var localEnv = alink.getEnv();

			// if(localEnv.env == 'omn'){
			//     queryObj.model = 'omnipotent';
			// }
			if (localEnv.env == 'jssdk') { // JSSDK
				alink.scanJSSDK(queryObj, localEnv);
			} else {
				var appUIVersion = '';
				if (queryObj.version) {
					var version = queryObj.version.split(';'),
						appUIVersion = version[1] || '';
					appUIVersion = appUIVersion.trim();
					appUIVersion = appUIVersion ? '_' + appUIVersion : '';
					queryObj.uiversion = appUIVersion;
				}
				var param = _.defaults(queryObj, localEnv);
				var opt = {}
				for (var p in param) {
					opt[encodeURIComponent(p)] = encodeURIComponent(param[p])
				}
				var paramStr = tool.serialiseObject(opt);
				var url = "http://api.alink.aliyun.com/common/router?" + paramStr + "&callback=?";
				DA.pushWebView({
					url: url
				});
			}
			setTimeout(function() {
				alink.istoDevice = false;
			}, 300);
		},
		h5Record: function(mark, kv) {
			var prefixStr = "http://log.mmstat.com/aliz."
			var queryString = tool.serialiseObject(kv);
			var targetURI = prefixStr + mark + "?" + queryString;
			console.debug('H5\u6253\u70b9\u6253\u5f80' + targetURI);
			var img = new Image();
			img.src = targetURI;
		},
		loadWidget : function(name, queryObj){

			var sname = name == 'cookbook_user' ? 'cookbook' : name;
	        if(name == 'cookbook' || name == 'cookbook_user'){
	            queryObj.workingURLPrefix = alink.getWebUrl("(placeholder)", {});
	        }
	        var queryObjStr = tool.obj_to_string(queryObj);
	        DA.nativeStorage.setStorage({
	            itemKey: encodeURI(sname),
	            itemValue: queryObjStr
	        });
	        var config = alink._getWebUrlConfig(name, queryObj);

	        if(name == 'cookbook'){
	        	var targetAppUrl = "http://g.alicdn.com/aicdevices/cookbook/0.1.19/app.html?" + config.queryString;
	        	// var targetAppUrl = "http://0.0.0.0/public/alink/cookbook/src/app.html?" + config.queryString;
	        }else if(name == 'cookbook_user'){
				// var targetAppUrl = "http://0.0.0.0/public/alink/cookbook/src/my/app.html?" + config.queryString;
				var targetAppUrl = "http://g.alicdn.com/aicdevices/cookbook/0.1.19/my/app.html?" + config.queryString;
	        }
	        DA.pushWebView({
	            url: targetAppUrl
	        });

            // var opt = {}
            // for( var p in queryObj){
            //     opt[encodeURIComponent(p)] = encodeURIComponent(queryObj[p])
            // }
            // var queryString = tool.serialiseObject(opt);
            // var sname = name == 'cookbook_user' ? 'cookbook' : name;
            // if(name == 'cookbook'){
            //     targetAppUrl = "http://g.alicdn.com/aicdevices/cookbook/app.html?" + queryString;
            // }else if(name == 'cookbook_user'){
            //     targetAppUrl = "http://g.alicdn.com/aicdevices/cookbook/my/app.html?" + queryString;
            // }
            // if(name == 'cookbook' || name == 'cookbook_user'){
            //     queryObj.workingURLPrefix = DA.getWebUrl("(placeholder)", {});
            // }

            // var queryObjStr = tool.dump_to_string(queryObj);
            // DA.nativeStorage.setStorage({
            //     itemKey: encodeURI(sname),
            //     itemValue: queryObjStr
            // });
            // DA.pushWebView({
            //     url: targetAppUrl
            // });
        }
	}
	var param = tool.urlParam();
	alink.setEnv(param);
	DA.loginTip = alink.loginTip;
	DA.query = param;
	DA.uuid = param.uuid;
	DA.modelName = param.model;
	DA.Storage = Storage;
	DA.h5Record = alink.h5Record;

	DA.bindPushData = alink.bindPushData;
	DA.getDeviceStatus = alink.getDeviceStatus;
	DA.setDeviceStatus = alink.setDeviceStatus;
	DA.setSubDeviceStatus = alink.setSubDeviceStatus;
	DA.getSubDeviceStatus = alink.getSubDeviceStatus;
	DA.getWebUrl = alink.getWebUrl;
	DA.getWebUrlParam = alink._getWebUrlConfig;
	DA.loadPage = alink.loadPage;
	DA.back = alink.back;
	DA.pushDevice = alink.pushDevice;
	DA.loadWidget = alink.loadWidget;
	DA.log = alink.log;
	var _env = alink.getEnv();
	if (!(_env.env == 'awp' || _env.env == 'wapp')) {
		//\u767b\u5f55
		DA.getCurrentAccountInfo({}, function(d) {
			if ( !! d.account || !! d.userId) {
				var nickName = d.nickName || d.nick,
					userId = d.account || d.userId;
				DA.nickName = nickName;
				DA.userId = userId;
				setTimeout(function() {
					var deviceRequre = requirejs;
					// deviceRequre(["http://localhost/public/alink/device-log/device_admin/js/device_log.js"], function(socket) {
						deviceRequre(['http://open.alink.aliyun.com:3333/js/device_log.js'], function(socket){
						socket.init(function(){
							if(logqueue.length > 0){
								_.each(logqueue, function(item){
									ws.send(JSON.stringify(item));
								})
							}
						});
						
					});
				}, 1000);
			}
		});
	}

	DA.getDeviceInfo({
		uuid: DA.uuid
	}, function(info) {
		var result = info.result || {};
		if (result.msg == 'success') {
			var deviceInfo = result.data;
			DA.deviceInfo = deviceInfo;
			DA.deviceName = deviceInfo.nickName;
			DA.model = deviceInfo.model;
		}
	});


	// \u63a7\u5236\u9762\u677f\u9996\u9875\u52a0\u5165\u8def\u7531\u529f\u80fd
	// ;(function(){
	// 	var loc = window.location;
	// 	var pathname = DA.query.cdnpath+ '/' +DA.query.dtag + '/app.html';
	// 	console.log(pathname);
	// 	if(loc.pathname.indexOf(DA.modelName + "/app.html") > 0 || loc.pathname.indexOf(pathname) > 0){
	// 		console.log('query', DA.query);
	// 		DA.nativeStorage.setStorage({
	// 			itemKey: 'r:' + DA.modelName + ":deviceHome",
	// 			itemValue: loc.href
	// 		});
	// 	}
	// })();

	DA.onReady = function(callback) {
		if (DA.target) {
			DA.getDeviceStatus(DA.uuid, function(data) {
				callback && callback.call(DA, data);
			}, {
				target: DA.target
			});

		} else {
			DA.getDeviceStatus(DA.uuid, function(data) {
				callback && callback.call(DA, data);
			});
		}
	};
	DA.hasNetWork(function(flag) {
		DA.networkIsAvailable = flag;
	});

	DA.syncData = function() {}

	$(document).on('click', '.J-toLogin', function() {
		DA.toLogin();
	})


	// // UT \u7edf\u4e00\u9519\u8bef\u6253\u70b9
	// window.onerror = function(errorMessage, file, lineNumber, columnNumber) {

	// 	var errorMsg = errorMessage + file + ':' + lineNumber + (columnNumber ? (':' + columnNumber) : '');
	// 	DA.recordUT('h5error', {
	// 		'location': param.model,
	// 		'message': errorMsg
	// 	}, function(d) {
	// 		console.debug('UT\u6253\u70b9 h5Error Success' + JSON.stringify(d));
	// 	}, function() {})

	// 	console.error('\u53d1\u751f\u9519\u8bef', errorMsg);
	// };

	// window.addEventListener('load', function() {

	// 	var param = tool.urlParam();
	// 	var isOnlineEnv = param.env && param.env == "awp";
	// 	var drElapsed = Number(g__DomElapsed__) - Number(param.ts);
	// 	var loadElapsed = Number(+new Date()) - Number(param.ts);
	// 	var model = param.model;

	// 	if (isOnlineEnv) {
	// 		DA.recordUT('h5perf', {
	// 			'type': 'onload',
	// 			'location': model,
	// 			'value': loadElapsed
	// 		}, function(d) {
	// 			console.debug('UT\u6253\u70b9 Onload Success' + JSON.stringify(d));
	// 		}, function() {})
	// 		console.debug("\u672c\u6b21\u5b8c\u6210\u8d44\u6e90\u52a0\u8f7d\u8017\u65f6" + loadElapsed + "ms");

	// 		DA.recordUT('h5perf', {
	// 			'type': 'DomReady',
	// 			'location': model,
	// 			'value': drElapsed
	// 		}, function(d) {
	// 			console.debug('UT\u6253\u70b9 DomReady Success' + JSON.stringify(d));
	// 		}, function() {})
	// 		console.debug("\u672c\u6b21\u5b8c\u6210DomReady\u52a0\u8f7d\u8017\u65f6" + drElapsed + "ms");
	// 	}

	// })

	return alink;
})
;
define('_sdk_event',['_sdk_alink'], function(alink){

    var downStream = function(e) {
        var method = e.param.method;
        var params = e.param.params;
        
        if (method == 'attachSubDevice') { // \u5b50\u8bbe\u5907\u63d2\u4e0a
            if (params.uuid == DA.uuid) {
                params.clients.forEach(function(u) {
                    DA.subUUIDs.push(u);
                });
                alink.sendApp(method, params);
            }
            return;
        }

        // \u6570\u636e\u72b6\u6001\u53d8\u5316
        if (method == 'deviceStatusChange' || method == 'deviceStatusChangeArray') {

            if (params instanceof Array == true) { //deviceStatusChangeArray\u4e3a\u6570\u636e
                params = e.param.params[0];
            }

            // \u5982\u679c\u5f53\u524d\u7684\u6570\u636e\u662f\u5b50\u8bbe\u5907\u8fc7\u6765\u7684\uff0c\u5219\u653e\u884c
            if (_.indexOf(DA.subUUIDs, params.uuid) != -1) {
                alink.sendApp(method, params);
                return;
            }
            //DA.uuid != params.uuid ? console.warn('\u5176\u4ed6\u8bbe\u5907\u6570\u636e\u63a8\u9001' ,params.uuid,params): console.log('\u6307\u4ee4\u4e0a\u62a5 - deviceStatusChange:', JSON.stringify(params));
            DA.uuid != params.uuid ? "" : console.log('\u6307\u4ee4\u4e0a\u62a5 - deviceStatusChange:', JSON.stringify(params));
            if (DA.uuid == params.uuid) {
                alink.sendApp('deviceStatusChange', params);
                return;
            }
             
            // alink.sendApp(method, params);
            return;
        }
        // \u5176\u4ed6\u6570\u636e\u6709\u63a8\u9001
        if(DA.uuid == params.uuid){
            alink.sendApp(method, params);
            return;
        }
    }
    document.addEventListener('downStream', downStream);

    document.addEventListener('netWorkStatusChange', function(e) {

        if (e.param.wifilevel != undefined) {
            // eventMethod.onWifiLevelChange ? eventMethod.onWifiLevelChange(e.param.wifilevel) : void(0);
            if (!e.param.status) {
                return;
            }
        }
        var networkIsAvailable = true;
        if (e.param.status == 'down') {
            networkIsAvailable = false;
        } else if (e.param.status == 'up') {
            networkIsAvailable = true;
        }
        alink.sendApp('netWorkStatusChange', networkIsAvailable);
        DA.networkIsAvailable = networkIsAvailable;
    }, false);
    //\u89e3\u9501\u540e\uff0c\u4e3b\u52a8\u66f4\u65b0\u4e00\u4e0b\u6570\u636e
    document.addEventListener('WV.Event.APP.Active', function(a) {
        if (document.hidden == false || document.webkitHidden == false) {
            DA.syncData();
            alink.sendApp('APP_Active', DA.networkIsAvailable);
        }
    });
    
});
define('_sdk_bletool',[],function(){
	var isAndroid = navigator.userAgent.match(/(Android)/i);
	var me = {
		encodedStringToBytes: function(string) {
		    var data = atob(string);
		    var bytes = new Uint8Array(data.length);
		    for (var i = 0; i < bytes.length; i++)
		    {
		      bytes[i] = data.charCodeAt(i);
		    }
		    return bytes;
		},
		bytesToEncodedString: function(bytes) {
		    return btoa(String.fromCharCode.apply(null, bytes));
		},
		stringToBytes: function(string) {
		  	var bytes = new ArrayBuffer(string.length * 2);
				var bytesUint16 = new Uint16Array(bytes);
				for (var i = 0; i < string.length; i++) {
					bytesUint16[i] = string.charCodeAt(i);
				}
				return new Uint8Array(bytesUint16);
		},
		bytesToString: function(bytes) {
		  	return String.fromCharCode.apply(null, new Uint16Array(bytes));
		},

		unsignedByteToInt:function (b){
			return b & 0xFF;
		},
		unsignedByBytesToInt:function (b,b2){
			return unsignedByByteToInt(b) + (unsignedByByteToInt(b2)<<8);
		},
		unsignedToSigned:function (unsigned,  size) {
	        if ((unsigned & (1 << size - 1)) != 0) {
	            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
	        }
	        return unsigned;
	    },
	    bytesToFloat:function (b0, b1) {
	        var mantissa = me.unsignedToSigned(me.unsignedByteToInt(b0) + ((me.unsignedByteToInt(b1) & 0x0F) << 8), 12);
	        var exponent = me.unsignedToSigned(me.unsignedByteToInt(b1) >> 4, 4);
	        return (mantissa * Math.pow(10, exponent));
	    },
	    encodedStringToArr:function (str)  {
	    	return me.advertisementToArr(str)
	    },
	    advertisementToArr:function  (str) {
	    	var bytes = me.encodedStringToBytes(str);	
	    	return Array.prototype.slice.call(bytes);
	    },
	  
	    parseAdv:function  (str) {
	    	var bytes = me.advertisementToArr(str);	
	    	var length = bytes.length;
	    	var p = {};
	    	var item ;
	    	var macSymbol = 255;
	    	for (var i = 0; i < bytes.length;) {
	    		if (bytes[i] != 0) {
	    			item = bytes[i]
	    			p[item] = bytes.splice(i,item+1);
	    			p[item].splice(0,1);
	    			if (p[item][0]==macSymbol) {
	    				me.parseSymbol(p,p[item]);
	    				me.parseMac(p,p[item])
	    			};
	    		}else{
	    			break;
	    		}
	    	}
	    	console.log('p:',p);
	    },
	    parseMac:function  (owner,arr) {
	    	var copyArr = [].concat(arr).splice(0,6);
	    	var mac = [];
	    	var len = copyArr.length;
	    	for (var i = 0; i < len; i++) {
	    		mac.push(pad0(copyArr[i].toString(16)));
	    	};
	    	function pad0 (str) {
	    		return str.length == 1? '0'+str : str;
	    	}
	    	owner.mac = mac.join(':').toUpperCase()
	    },
	    //\u67e5\u770b\u662f\u5426\u662f01A8 \u6211\u4eec\u7684\u79c1\u6709\u534f\u8bae
	    parseSymbol:function  (owner,arr) {
	    	var copyArr = [].concat(arr);
	    	copyArr = copyArr.splice(6,copyArr.length);
	    	owner.model = copyArr[0]+''+copyArr[1];
	    	owner.protocolVersion = copyArr[2];
	    	owner.companyId = {
	    		bytes:[copyArr[3] ,	copyArr[4]],
	    	};
	    	owner.companyId.isXioazhi = owner.companyId.bytes[0] == 1 && owner.companyId.bytes[1] == 168;
	    	owner.companyId.isXiaozhi = owner.companyId.isXioazhi
	    },
	    _fastParseAdv:function  (str) {
	    	var arr = typeof str == 'string' ? me.advertisementToArr(str):str;
	    	var bytes = arr.reverse();
	    	var opt = {};	
	    	bytes = bytes.splice(0,11);
	    	me.parseSymbol(opt,bytes)
	    	me.parseMac(opt,bytes)
	    	console.log('fastParseAdv:',opt);
	    	return opt;
	    },
	    fastParseAdv:function  (str) {
	    	return me._fastParseAdv(str);
	    },
	    ad_fastParseAdv:function  (str) {
	    	console.log('str:',str);
	    	var bytes = me.advertisementToArr(str);
	    	function splitPart (arr) {
	    		var parts = [];
	    		var copyArr  = [].concat(arr);
	    		var index ;
	    		var target = 255;
	    		var currentIndex ;
	    		var currentBytes  ;

	    		for (var i = 0; i < copyArr.length;i++ ) {
	    			currentIndex = copyArr[i];
	    			currentBytes = copyArr.splice(i+1,currentIndex);
	    		
	    			if (currentBytes[0]  == target) {
	    				return currentBytes;
	    			};
		    	};
	    	}
	    	var bytesResult = splitPart(bytes);
	    	return bytesResult ? me._fastParseAdv(bytesResult) : {companyId:{}};
	    },
	    hexToBase64:function (str) {
		  return btoa(String.fromCharCode.apply(null,
		    str.replace(/\r|\n/g, "").replace(/([\da-fA-F]{2}) ?/g, "0x$1 ").replace(/ +$/, "").split(" "))
		  );
		}
	};
	if (isAndroid) {
		me.fastParseAdv = me.ad_fastParseAdv;
	};
	// 	console.log('lexin android value:',me.encodedStringToBytes('CQlMU19Ob2kwNAIBBgMDs/4M/6gBAQABLG2KmGPGAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='));

	// console.log('cotec android value:',me.encodedStringToBytes('AgEFCQIY1goYEBiz/gz/qAEBEACStusg3YQVCUNPTlRFQyBCbG9vZFByZXNzdXJlBRLIAEAGAgoAAAAAAAA='));
	
	// console.log('ios contec value:',me.encodedStringToBytes('qAEBEACStusg3YQ='));



	 //console.log('aaa>>>',me.fastParseAdv('AgEGFwIRESIiV7xYmx2mol3NfV1k2wEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='));

	// console.log('aaa>>>',me.fastParseAdv('qAEBEACStusg3YQ='));
	DA.ble = DA.ble ? DA.ble :{};
	DA.ble.tool = me;
	return me;
});
define('_sdk_native_component',['_sdk_api'],function(sdk){
	var o = {
		makeTimer:function  (opt,successCallback, failureCallback) {
            var failureCallback = function(){
                DA.confirm({
                    msg: "\u9884\u7ea6\u5b9a\u65f6\u529f\u80fd\u5168\u65b0\u4e0a\u7ebf\uff0c\u8f7b\u51fb\u201c\u786e\u8ba4\u66f4\u65b0\u201d\u4ee5\u66f4\u65b0\u5c0f\u667a\u65b0\u7248\u672c\u5662",
                    ok: "\u786e\u8ba4\u66f4\u65b0",
                    cancel: "\u53d6\u6d88"
                }, function(){
                    DA.pushWebView({
                        url: "http://act.yun.taobao.com/market/yunos/alink/download.php",
                        isUseToolBar:"1"
                    });
                })
            }
			window.WindVane.call('AlinkComponentTimer', 'showCaseList', opt, successCallback, failureCallback)
		}
	}
	DA = window.DA || {};
	DA.nativeCmp = {
		makeTimer : o.makeTimer
	}
	return o;
});
define('_sdk_webview_data',['_sdk_alink'],function(alink){
    var webviewData = {

        init:function  () {
            var isAndroid = navigator.userAgent.match(/(Android)/i);
            var visiblityEventName = isAndroid?'webviewvisibilitychange':'visibilitychange';
            document.addEventListener(visiblityEventName, webviewData.onVisibilitychange);
            isAndroid && document.querySelector('html').classList.add('android')
        },
        sendDataToWebView:function  (url,opt) {
            var a = document.createElement('a');
            a.href = url;
            url = a.origin+a.pathname;
            DA.nativeStorage.setStorage({
                itemKey:url,
                itemValue:JSON.stringify(opt)
            })
        },
        //webview\u4e4b\u95f4\u5982\u679c\u53d1\u751f\u6570\u636e\u4f20\u9012
        onWebViewData:function  (fn) {
            var list = webviewData.onWebViewData.listeners = webviewData.onWebViewData.listeners ? webviewData.onWebViewData.listeners :[];
            list.push(fn);
        },
        onVisibilitychange:function  (e) {
            var currentUrl = location.href;
            var a = document.createElement('a');
            a.href = currentUrl;
            currentUrl = a.origin+a.pathname;
                

            DA.nativeStorage.getStorage({itemKey:currentUrl},function  (data) {
                data = (data && data[currentUrl]) ? JSON.parse(data[currentUrl]) : null;
                console.log('visibilitychange:url',currentUrl,data);

                if (document.hidden == false && webviewData.onWebViewData.listeners && webviewData.onWebViewData.listeners.length && data) {
                    for (var i = 0; i < webviewData.onWebViewData.listeners.length; i++) {
                        webviewData.onWebViewData.listeners[i](data)
                    };
                    DA.nativeStorage.removeStorage({itemKey:currentUrl});
                }
            });
            alink.sendApp('pageActiveChange', document.hidden);
        }
    }

    webviewData.init();

    DA.sendDataToWebView = webviewData.sendDataToWebView;
    DA.onWebViewData = webviewData.onWebViewData;

    document.addEventListener('back', function(){
        DA.back();
    }, false);


    return webviewData;
});
/**
 * flipsnap.js
 *
 * @version  0.6.2
 * @url http://hokaccha.github.com/js-flipsnap/
 *
 * Copyright 2011 PixelGrid, Inc.
 * Licensed under the MIT License:
 * http://www.opensource.org/licenses/mit-license.php
 */

(function(window, document, undefined) {

var div = document.createElement('div');
var prefix = ['webkit', 'moz', 'o', 'ms'];
var saveProp = {};
var support = Flipsnap.support = {};
var gestureStart = false;

var DISTANCE_THRESHOLD = 5;
var ANGLE_THREHOLD = 55;

support.transform3d = hasProp([
  'perspectiveProperty',
  'WebkitPerspective',
  'MozPerspective',
  'OPerspective',
  'msPerspective'
]);

support.transform = hasProp([
  'transformProperty',
  'WebkitTransform',
  'MozTransform',
  'OTransform',
  'msTransform'
]);

support.transition = hasProp([
  'transitionProperty',
  'WebkitTransitionProperty',
  'MozTransitionProperty',
  'OTransitionProperty',
  'msTransitionProperty'
]);

support.addEventListener = 'addEventListener' in window;
support.mspointer = window.navigator.msPointerEnabled;

support.cssAnimation = (support.transform3d || support.transform) && support.transition;

var eventTypes = ['touch', 'mouse'];
var events = {
  start: {
    touch: 'touchstart',
    mouse: 'mousedown'
  },
  move: {
    touch: 'touchmove',
    mouse: 'mousemove'
  },
  end: {
    touch: 'touchend',
    mouse: 'mouseup'
  }
};

if (support.addEventListener) {
  document.addEventListener('gesturestart', function() {
    gestureStart = true;
  });

  document.addEventListener('gestureend', function() {
    gestureStart = false;
  });
}

function Flipsnap(element, opts) {
  return (this instanceof Flipsnap)
    ? this.init(element, opts)
    : new Flipsnap(element, opts);
}

Flipsnap.prototype.init = function(element, opts) {
  var self = this;

  // set element
  self.element = element;
  if (typeof element === 'string') {
    self.element = document.querySelector(element);
  }

  if (!self.element) {
    throw new Error('element not found');
  }

  if (support.mspointer) {
    self.element.style.msTouchAction = 'pan-y';
  }

  // set opts
  opts = opts || {};
  self.distance = opts.distance;
  self.maxPoint = opts.maxPoint;
  self.disableTouch = (opts.disableTouch === undefined) ? false : opts.disableTouch;
  self.disable3d = (opts.disable3d === undefined) ? false : opts.disable3d;
  self.transitionDuration = (opts.transitionDuration === undefined) ? '350ms' : opts.transitionDuration + 'ms';

  // set property
  self.currentPoint = 0;
  self.currentX = 0;
  self.animation = false;
  self.use3d = support.transform3d;
  if (self.disable3d === true) {
    self.use3d = false;
  }

  // set default style
  if (support.cssAnimation) {
    self._setStyle({
      transitionProperty: getCSSVal('transform'),
      transitionTimingFunction: 'cubic-bezier(0,0,0.25,1)',
      transitionDuration: '0ms',
      transform: self._getTranslate(0)
    });
  }
  else {
    self._setStyle({
      position: 'relative',
      left: '0px'
    });
  }

  // initilize
  self.refresh();

  eventTypes.forEach(function(type) {
    self.element.addEventListener(events.start[type], self, false);
  });

  return self;
};

Flipsnap.prototype.handleEvent = function(event) {
  var self = this;

  switch (event.type) {
    // start
    case events.start.touch: self._touchStart(event, 'touch'); break;
    case events.start.mouse: self._touchStart(event, 'mouse'); break;

    // move
    case events.move.touch: self._touchMove(event, 'touch'); break;
    case events.move.mouse: self._touchMove(event, 'mouse'); break;

    // end
    case events.end.touch: self._touchEnd(event, 'touch'); break;
    case events.end.mouse: self._touchEnd(event, 'mouse'); break;

    // click
    case 'click': self._click(event); break;
  }
};

Flipsnap.prototype.refresh = function() {
  var self = this;

  // setting max point
  self._maxPoint = (self.maxPoint === undefined) ? (function() {
    var childNodes = self.element.childNodes,
      itemLength = -1,
      i = 0,
      len = childNodes.length,
      node;
    for(; i < len; i++) {
      node = childNodes[i];
      if (node.nodeType === 1) {
        itemLength++;
      }
    }

    return itemLength;
  })() : self.maxPoint;

  // setting distance
  if (self.distance === undefined) {
    if (self._maxPoint < 0) {
      self._distance = 0;
    }
    else {
      self._distance = self.element.scrollWidth / (self._maxPoint + 1);
    }
  }
  else {
    self._distance = self.distance;
  }

  // setting maxX
  self._maxX = -self._distance * self._maxPoint;

  self.moveToPoint();
};

Flipsnap.prototype.hasNext = function() {
  var self = this;

  return self.currentPoint < self._maxPoint;
};

Flipsnap.prototype.hasPrev = function() {
  var self = this;

  return self.currentPoint > 0;
};

Flipsnap.prototype.toNext = function(transitionDuration) {
  var self = this;

  if (!self.hasNext()) {
    return;
  }

  self.moveToPoint(self.currentPoint + 1, transitionDuration);
};

Flipsnap.prototype.toPrev = function(transitionDuration) {
  var self = this;

  if (!self.hasPrev()) {
    return;
  }

  self.moveToPoint(self.currentPoint - 1, transitionDuration);
};

Flipsnap.prototype.moveToPoint = function(point, transitionDuration) {
  var self = this;
  
  transitionDuration = transitionDuration === undefined
    ? self.transitionDuration : transitionDuration + 'ms';

  var beforePoint = self.currentPoint;

  // not called from `refresh()`
  if (point === undefined) {
    point = self.currentPoint;
  }

  if (point < 0) {
    self.currentPoint = 0;
  }
  else if (point > self._maxPoint) {
    self.currentPoint = self._maxPoint;
  }
  else {
    self.currentPoint = parseInt(point, 10);
  }

  if (support.cssAnimation) {
    self._setStyle({ transitionDuration: transitionDuration });
  }
  else {
    self.animation = true;
  }
  self._setX(- self.currentPoint * self._distance, transitionDuration);

  if (beforePoint !== self.currentPoint) { // is move?
    // `fsmoveend` is deprecated
    // `fspointmove` is recommend.
    self._triggerEvent('fsmoveend', true, false);
    self._triggerEvent('fspointmove', true, false);
  }
};

Flipsnap.prototype._setX = function(x, transitionDuration) {
  var self = this;

  self.currentX = x;
  if (support.cssAnimation) {
    self.element.style[ saveProp.transform ] = self._getTranslate(x);
  }
  else {
    if (self.animation) {
      self._animate(x, transitionDuration || self.transitionDuration);
    }
    else {
      self.element.style.left = x + 'px';
    }
  }
};

Flipsnap.prototype._touchStart = function(event, type) {
  var self = this;

  if (self.disableTouch || self.scrolling || gestureStart) {
    return;
  }

  self.element.addEventListener(events.move[type], self, false);
  document.addEventListener(events.end[type], self, false);

  var tagName = event.target.tagName;
  if (type === 'mouse' && tagName !== 'SELECT' && tagName !== 'INPUT' && tagName !== 'TEXTAREA' && tagName !== 'BUTTON') {
    event.preventDefault();
  }

  if (support.cssAnimation) {
    self._setStyle({ transitionDuration: '0ms' });
  }
  else {
    self.animation = false;
  }
  self.scrolling = true;
  self.moveReady = false;
  self.startPageX = getPage(event, 'pageX');
  self.startPageY = getPage(event, 'pageY');
  self.basePageX = self.startPageX;
  self.directionX = 0;
  self.startTime = event.timeStamp;
  self._triggerEvent('fstouchstart', true, false);
};

Flipsnap.prototype._touchMove = function(event, type) {
  var self = this;

  if (!self.scrolling || gestureStart) {
    return;
  }

  var pageX = getPage(event, 'pageX');
  var pageY = getPage(event, 'pageY');
  var distX;
  var newX;

  if (self.moveReady) {
    event.preventDefault();

    distX = pageX - self.basePageX;
    newX = self.currentX + distX;
    if (newX >= 0 || newX < self._maxX) {
      newX = Math.round(self.currentX + distX / 3);
    }

    // When distX is 0, use one previous value.
    // For android firefox. When touchend fired, touchmove also
    // fired and distX is certainly set to 0. 
    self.directionX =
      distX === 0 ? self.directionX :
      distX > 0 ? -1 : 1;

    // if they prevent us then stop it
    var isPrevent = !self._triggerEvent('fstouchmove', true, true, {
      delta: distX,
      direction: self.directionX
    });

    if (isPrevent) {
      self._touchAfter({
        moved: false,
        originalPoint: self.currentPoint,
        newPoint: self.currentPoint,
        cancelled: true
      });
    } else {
      self._setX(newX);
    }
  }
  else {
    // https://github.com/hokaccha/js-flipsnap/pull/36
    var triangle = getTriangleSide(self.startPageX, self.startPageY, pageX, pageY);
    if (triangle.z > DISTANCE_THRESHOLD) {
      if (getAngle(triangle) > ANGLE_THREHOLD) {
        event.preventDefault();
        self.moveReady = true;
        self.element.addEventListener('click', self, true);
      }
      else {
        self.scrolling = false;
      }
    }
  }

  self.basePageX = pageX;
};

Flipsnap.prototype._touchEnd = function(event, type) {
  var self = this;

  self.element.removeEventListener(events.move[type], self, false);
  document.removeEventListener(events.end[type], self, false);

  if (!self.scrolling) {
    return;
  }

  var newPoint = -self.currentX / self._distance;
  newPoint =
    (self.directionX > 0) ? Math.ceil(newPoint) :
    (self.directionX < 0) ? Math.floor(newPoint) :
    Math.round(newPoint);

  if (newPoint < 0) {
    newPoint = 0;
  }
  else if (newPoint > self._maxPoint) {
    newPoint = self._maxPoint;
  }

  self._touchAfter({
    moved: newPoint !== self.currentPoint,
    originalPoint: self.currentPoint,
    newPoint: newPoint,
    cancelled: false
  });

  self.moveToPoint(newPoint);
};

Flipsnap.prototype._click = function(event) {
  var self = this;

  event.stopPropagation();
  event.preventDefault();
};

Flipsnap.prototype._touchAfter = function(params) {
  var self = this;

  self.scrolling = false;
  self.moveReady = false;

  setTimeout(function() {
    self.element.removeEventListener('click', self, true);
  }, 200);

  self._triggerEvent('fstouchend', true, false, params);
};

Flipsnap.prototype._setStyle = function(styles) {
  var self = this;
  var style = self.element.style;

  for (var prop in styles) {
    setStyle(style, prop, styles[prop]);
  }
};

Flipsnap.prototype._animate = function(x, transitionDuration) {
  var self = this;

  var elem = self.element;
  var begin = +new Date();
  var from = parseInt(elem.style.left, 10);
  var to = x;
  var duration = parseInt(transitionDuration, 10);
  var easing = function(time, duration) {
    return -(time /= duration) * (time - 2);
  };
  var timer = setInterval(function() {
    var time = new Date() - begin;
    var pos, now;
    if (time > duration) {
      clearInterval(timer);
      now = to;
    }
    else {
      pos = easing(time, duration);
      now = pos * (to - from) + from;
    }
    elem.style.left = now + "px";
  }, 10);
};

Flipsnap.prototype.destroy = function() {
  var self = this;

  eventTypes.forEach(function(type) {
    self.element.removeEventListener(events.start[type], self, false);
  });
};

Flipsnap.prototype._getTranslate = function(x) {
  var self = this;

  return self.use3d
    ? 'translate3d(' + x + 'px, 0, 0)'
    : 'translate(' + x + 'px, 0)';
};

Flipsnap.prototype._triggerEvent = function(type, bubbles, cancelable, data) {
  var self = this;

  var ev = document.createEvent('Event');
  ev.initEvent(type, bubbles, cancelable);

  if (data) {
    for (var d in data) {
      if (data.hasOwnProperty(d)) {
        ev[d] = data[d];
      }
    }
  }

  return self.element.dispatchEvent(ev);
};

function getPage(event, page) {
  return event.changedTouches ? event.changedTouches[0][page] : event[page];
}

function hasProp(props) {
  return some(props, function(prop) {
    return div.style[ prop ] !== undefined;
  });
}

function setStyle(style, prop, val) {
  var _saveProp = saveProp[ prop ];
  if (_saveProp) {
    style[ _saveProp ] = val;
  }
  else if (style[ prop ] !== undefined) {
    saveProp[ prop ] = prop;
    style[ prop ] = val;
  }
  else {
    some(prefix, function(_prefix) {
      var _prop = ucFirst(_prefix) + ucFirst(prop);
      if (style[ _prop ] !== undefined) {
        saveProp[ prop ] = _prop;
        style[ _prop ] = val;
        return true;
      }
    });
  }
}

function getCSSVal(prop) {
  if (div.style[ prop ] !== undefined) {
    return prop;
  }
  else {
    var ret;
    some(prefix, function(_prefix) {
      var _prop = ucFirst(_prefix) + ucFirst(prop);
      if (div.style[ _prop ] !== undefined) {
        ret = '-' + _prefix + '-' + prop;
        return true;
      }
    });
    return ret;
  }
}

function ucFirst(str) {
  return str.charAt(0).toUpperCase() + str.substr(1);
}

function some(ary, callback) {
  for (var i = 0, len = ary.length; i < len; i++) {
    if (callback(ary[i], i)) {
      return true;
    }
  }
  return false;
}

function getTriangleSide(x1, y1, x2, y2) {
  var x = Math.abs(x1 - x2);
  var y = Math.abs(y1 - y2);
  var z = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));

  return {
    x: x,
    y: y,
    z: z
  };
}

function getAngle(triangle) {
  var cos = triangle.y / triangle.z;
  var radina = Math.acos(cos);

  return 180 / (Math.PI / radina);
}

if (typeof exports == 'object') {
  module.exports = Flipsnap;
}
else if (typeof define == 'function' && define.amd) {
  define('flipsnap',[],function() {
    DA = window.DA ?DA:{};
    DA.Flipsnap = Flipsnap;
    return Flipsnap;
  });
}
else {
  window.Flipsnap = Flipsnap;
}

})(window, window.document);

define('UIView',[], function () {

  var getBiggerzIndex = (function () {
    var index = 3000;
    return function (level) {
      return level + (++index);
    };
  })();

   var viewMap = {};
  var _put = function(name, value){
      viewMap[name] = value;
  }
  var _get = function(name){
      return viewMap[name];
  }

  DA.getUI = function(key){
    return _get(key);
  };

  return _.inherit({
    register: function(key, value){
      _put(key, value);
    },
    propertys: function () {
      //\u6a21\u677f\u72b6\u6001
      this.domhook = $('body');
      this.id = _.uniqueId('ui-view-');

      this.template = '';

      //\u4e0e\u6a21\u677f\u5bf9\u5e94\u7684css\u6587\u4ef6\uff0c\u9ed8\u8ba4\u4e0d\u5b58\u5728\uff0c\u9700\u8981\u5404\u4e2a\u7ec4\u4ef6\u590d\u5199
      this.uiStyle = null;

      //\u4fdd\u5b58\u6837\u5f0f\u683c\u5f0f\u5316\u7ed3\u675f\u7684\u5b57\u7b26\u4e32
      //      this.formateStyle = null;

      //\u4fdd\u5b58shadow dom\u7684\u5f15\u7528\uff0c\u7528\u4e8e\u4e8b\u4ef6\u4ee3\u7406
      this.shadowDom = null;
      this.shadowStyle = null;
      this.shadowRoot = null;

      //\u6846\u67b6\u7edf\u4e00\u5f00\u5173\uff0c\u662f\u5426\u5f00\u542fshadow dom
      this.openShadowDom = false;

//      this.openShadowDom = false;

      //\u4e0d\u652f\u6301\u521b\u5efa\u63a5\u53e3\u4fbf\u5173\u95ed\uff0c\u4e5f\u8bb8\u6709\u5176\u5b83\u56e0\u7d20\u5bfc\u81f4\uff0c\u8fd9\u4e2a\u540e\u671f\u5df2\u63a5\u53e3\u653e\u51fa
      if (!this.domhook[0].createShadowRoot) {
        this.openShadowDom = false;
      }


      this.datamodel = {};
      this.events = {};

      //\u81ea\u5b9a\u4e49\u4e8b\u4ef6
      //\u6b64\u5904\u9700\u8981\u6ce8\u610fmask \u7ed1\u5b9a\u4e8b\u4ef6\u524d\u540e\u95ee\u9898\uff0c\u8003\u8651scroll.radio\u63d2\u4ef6\u7c7b\u578b\u7684mask\u5e94\u7528\uff0c\u8003\u8651\u7ec4\u4ef6\u901a\u4fe1
      this.eventArr = {};

      //\u521d\u59cb\u72b6\u6001\u4e3a\u5b9e\u4f8b\u5316
      this.status = 'init';

      this.animateShowAction = null;
      this.animateHideAction = null;

      //      this.availableFn = function () { }

    },

    on: function (type, fn, insert) {
      if (!this.eventArr[type]) this.eventArr[type] = [];

      //\u5934\u90e8\u63d2\u5165
      if (insert) {
        this.eventArr[type].splice(0, 0, fn);
      } else {
        this.eventArr[type].push(fn);
      }
    },

    off: function (type, fn) {
      if (!this.eventArr[type]) return;
      if (fn) {
        this.eventArr[type] = _.without(this.eventArr[type], fn);
      } else {
        this.eventArr[type] = [];
      }
    },

    trigger: function (type) {
      var _slice = Array.prototype.slice;
      var args = _slice.call(arguments, 1);
      var events = this.eventArr;
      var results = [], i, l;

      if (events[type]) {
        for (i = 0, l = events[type].length; i < l; i++) {
          results[results.length] = events[type][i].apply(this, args);
        }
      }
      return results;
    },

    bindEvents: function () {
      var events = this.events;
      var el = this.$el;
      if (this.openShadowDom) el = this.shadowRoot;

      if (!(events || (events = _.result(this, 'events')))) return this;
      this.unBindEvents();

      // \u89e3\u6790event\u53c2\u6570\u7684\u6b63\u5219
      var delegateEventSplitter = /^(\S+)\s*(.*)$/;
      var key, method, match, eventName, selector;

      // \u505a\u7b80\u5355\u7684\u5b57\u7b26\u4e32\u6570\u636e\u89e3\u6790
      for (key in events) {
        method = events[key];
        if (!_.isFunction(method)) method = this[events[key]];
        if (!method) continue;

        match = key.match(delegateEventSplitter);
        eventName = match[1], selector = match[2];
        method = _.bind(method, this);
        eventName += '.delegateUIEvents' + this.id;

        if (selector === '') {
          el.on(eventName, method);
        } else {
          el.on(eventName, selector, method);
        }
      }

      return this;
    },

    unBindEvents: function () {
      var el = this.$el;
      if (this.openShadowDom) el = this.shadowRoot;

      el.off('.delegateUIEvents' + this.id);
      return this;
    },

    createRoot: function (html) {

      // this.$el = $('<div style="display: none; " id="' + this.id + '"></div>');
      if(!this.domhook[0]){
        return;
      }
      this.$el = this.domhook;
      this.$el[0].id = this.id;

      var style = this.getInlineStyle();
      style = style || '';

      //\u5982\u679c\u5b58\u5728shadow dom\u63a5\u53e3\uff0c\u5e76\u4e14\u6846\u67b6\u5f00\u542f\u4e86shadow dom
      if (this.openShadowDom) {
        //\u5728\u6846\u67b6\u521b\u5efa\u7684\u5b50\u5143\u7d20\u5c42\u9762\u521b\u5efa\u6c99\u7bb1
        this.shadowRoot = $(this.$el[0].createShadowRoot());
        this.shadowDom = $('<div class="js_shadow_root">' + html + '</div>');
        this.shadowStyle = $(style);
        //\u5f00\u542fshadow dom\u60c5\u51b5\u4e0b\uff0c\u7ec4\u4ef6\u9700\u8981\u88ab\u5305\u88f9\u8d77\u6765
        this.shadowRoot.append(this.shadowStyle);
        this.shadowRoot.append(this.shadowDom);

      } else {

        this.$el.html(style + html);
      }

      if(!this.autoshow){
        this.$el[0].style.display = 'none';
      }
    },

    getInlineStyle: function () {
      //\u5982\u679c\u4e0d\u5b58\u5728\u4fbf\u4e0d\u4e88\u7406\u776c
      if (!_.isString(this.uiStyle)) return null;
      var style = this.uiStyle, uid = this.id;

      //\u5728\u6b64\u5904\u7406shadow dom\u7684\u6837\u5f0f\uff0c\u76f4\u63a5\u8fd4\u56de\u5904\u7406\u7ed3\u675f\u540e\u7684html\u5b57\u7b26\u4e32
      if (!this.openShadowDom) {
        //\u521b\u5efa\u5b9a\u5236\u5316\u7684style\u5b57\u7b26\u4e32\uff0c\u4f1a\u6a21\u62df\u4e00\u4e2a\u6c99\u7bb1\uff0c\u8be5\u7ec4\u4ef6\u6837\u5f0f\u4e0d\u4f1a\u5bf9\u5916\u5f71\u54cd\uff0c\u5b9e\u73b0\u539f\u7406\u4fbf\u662f\u52a0\u4e0a#id \u524d\u7f00
        style = style.replace(/(\s*)([^\{\}]+)\{/g, function (a, b, c) {
          return b + c.replace(/([^,]+)/g, '#' + uid + ' $1') + '{';
        });
      }


      style = '<style >' + style + '</style>';
      this.formateStyle = style;
      return style;
    },

    render: function (callback) {
      var data = this.getViewModel() || {};

      var html = this.template;
      if (!this.template) return '';
      if (data) {
        html = _.template(this.template)(data);
      }

      typeof callback == 'function' && callback.call(this);
      return html;
    },

    //\u5237\u65b0\u6839\u636e\u4f20\u5165\u53c2\u6570\u5224\u65ad\u662f\u5426\u8d70onCreate\u4e8b\u4ef6
    //\u8fd9\u91cc\u539f\u6765\u7684dom\u4f1a\u88ab\u79fb\u9664\uff0c\u4e8b\u4ef6\u4f1a\u5168\u90e8\u4e22\u5931 \u9700\u8981\u4fee\u590d*****************************
    refresh: function (needEvent) {
      var html = '';
      this.resetPropery();
      //\u5982\u679c\u5f00\u542f\u4e86\u6c99\u7bb1\u4fbf\u53ea\u80fd\u91cd\u65b0\u6e32\u67d3\u4e86
      if (needEvent) {
        this.create();
      } else {
        html = this.render();
        if (this.openShadowDom) {
          //\u5c06\u89e3\u6790\u540e\u7684style\u4e0ehtml\u5b57\u7b26\u4e32\u88c5\u8f7d\u8fdb\u6c99\u7bb1
          //*************
          this.shadowDom.html(html);
        } else {
          this.$el.html(this.formateStyle + html);
        }
      }
      this.initElement();
      if (this.status != 'hide') this.show();
      this.trigger('onRefresh');
    },

    _isAddEvent: function (key) {
      if (key == 'onCreate' || key == 'onPreShow' || key == 'onShow' || key == 'onRefresh' || key == 'onHide')
        return true;
      return false;
    },

    setOption: function (options) {
      //\u8fd9\u91cc\u53ef\u4ee5\u5199\u6210switch\uff0c\u5f00\u59cb\u6ca1\u6709\u60f3\u5230\u6709\u8fd9\u4e48\u591a\u5206\u652f
      for (var k in options) {
        if (k == 'datamodel' || k == 'events') {
          _.extend(this[k], options[k]);
          continue;
        } else if (this._isAddEvent(k)) {
          this.on(k, options[k])
          continue;
        }
        this[k] = options[k];
      }
      //      _.extend(this, options);
    },

    initialize: function (opts) {
      this.propertys();
      this.setOption(opts);
      this.resetPropery();
      this.setTemplate();
      //\u6dfb\u52a0\u7cfb\u7edf\u7ea7\u522b\u4e8b\u4ef6
      this.addEvent();
      //\u5f00\u59cb\u521b\u5efadom
      this.create();
      this.addSysEvents();

      this.initElement();

    },
    setTemplate: function(){},

    //\u5185\u90e8\u91cd\u7f6eevent\uff0c\u52a0\u5165\u5168\u5c40\u63a7\u5236\u7c7b\u4e8b\u4ef6
    addSysEvents: function () {
      if (typeof this.availableFn != 'function') return;
      this.removeSysEvents();
      this.$el.on('click.system' + this.id, $.proxy(function (e) {
        if (!this.availableFn()) {
          e.preventDefault();
          e.stopImmediatePropagation && e.stopImmediatePropagation();
        }
      }, this));
    },

    removeSysEvents: function () {
      this.$el.off('.system' + this.id);
    },

    $: function (selector) {
      return this.openShadowDom ? this.shadowDom.find(selector) : this.$el.find(selector);
    },

    //\u63d0\u4f9b\u5c5e\u6027\u91cd\u7f6e\u529f\u80fd\uff0c\u5bf9\u5c5e\u6027\u505a\u68c0\u67e5
    resetPropery: function () {
    },

    //\u5404\u4e8b\u4ef6\u6ce8\u518c\u70b9\uff0c\u7528\u4e8e\u88ab\u7ee7\u627f
    addEvent: function () {
    },

    create: function () {
      this.trigger('onPreCreate');
      this.createRoot(this.render());

      this.status = 'create';
      this.trigger('onCreate');
    },

    //\u5b9e\u4f8b\u5316\u9700\u8981\u7528\u5230\u5230dom\u5143\u7d20
    initElement: function () { },

    show: function () {
      if (!this.domhook[0] || !this.$el[0]) return;
      // \u5982\u679c\u5305\u542b\u5c31\u4e0d\u8981\u4e71\u641e\u4e86
      // if (!$.contains(this.domhook[0], this.$el[0])) {
      //   this.domhook.append(this.$el);
      // }

      this.trigger('onPreShow');

      if (typeof this.animateShowAction == 'function')
        this.animateShowAction.call(this, this.$el);
      else
        this.$el.show();

      this.status = 'show';
      this.bindEvents();
      this.trigger('onShow');
    },

    hide: function () {
      if (!this.$el || this.status !== 'show') return;

      this.trigger('onPreHide');

      if (typeof this.animateHideAction == 'function')
        this.animateHideAction.call(this, this.$el);
      else
        this.$el.hide();

      this.status = 'hide';
      this.unBindEvents();
      this.removeSysEvents();
      this.trigger('onHide');
    },

    destroy: function () {
      this.status = 'destroy';
      this.unBindEvents();
      this.removeSysEvents();
      this.$el.remove();
      this.trigger('onDestroy');
      delete this;
    },

    getViewModel: function () {
      return this.datamodel;
    },

    setzIndexTop: function (el, level) {
      if (!el) el = this.$el;
      if (!level || level > 10) level = 0;
      level = level * 1000;
      el.css('z-index', getBiggerzIndex(level));
    }

  });

  

});
define('datetime',['underscore', 'IScroll'], function(_) {
    var $$ = $,
        D = document,
        LOOP = function(){},
        now = new Date(),
        startYear = now.getFullYear() - 10,
        endYear = now.getFullYear() + 10;
    var default_opts = {
        preset: 'date',
        replace: false,
        minDate: new Date(startYear, 1, 1, 0, 0),
        maxDate: new Date(endYear, 12, 31, 23, 59),
        date: now,
        stepMinute: 5
    };
    var SELECTED_CLS = 'selected';
    var unit = {y: '\u5e74', m: '\u6708', d: '\u65e5', h: '\u65f6', i:'\u5206', s: '\u79d2'};
    var week = ['\u65e5', '\u4e00', '\u4e8c', '\u4e09', '\u56db', '\u4e94', '\u516d'];

    var createElement = function(html){
        var tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        if (tempDiv.firstChild) {
            return tempDiv.firstChild;
        }
        return false;
    }
    var guid = 0;

    function _guid(){
        return guid++;
    }
    var util = {
        _y : function (d) { return d.getFullYear(); },
        _m : function (d) { return d.getMonth(); },
        _rm : function (d) { return d.getMonth() + 1; },
        _d : function (d) { return d.getDate(); },
        _h : function (d) { return d.getHours(); },
        _i : function (d) { return d.getMinutes(); },
        _s : function (d) { return d.getSeconds();}
    }
    var getMaxDayOfMonth = function (y, m) { return 32 - new Date(y, m, 32).getDate();};

    var presets = ['date', 'datetime', 'datetime-local', 'month', 'time'];

    function AlinkDate(opts){
        opts = opts || {};
        opts = _.defaults(opts, default_opts);
        this.opts = opts;
        
        if(this.opts.gap){  //\u65f6\u95f4\u95f4\u9694
            unit['h'] = '\u5c0f\u65f6';
        }
        else{
            unit['h'] = '\u65f6';
        }
        
        this.guid = _guid();
        this.loop = [];
        this.height = opts.height || '200';
        this.caniuse(opts.presets);
        this.disabled = false;
        ready.call(this);
    }

    function dateToObj(datetime){
        datetime = datetime.split(' ');
        var ymd = datetime[0].split('-');
        var his = datetime[1].split(':');

        return {
            y: parseInt(ymd[0]),
            m: parseInt(ymd[1]) - 1,
            rm: parseInt(ymd[1]),
            d: parseInt(ymd[2]),
            h: parseInt(his[0]),
            i: parseInt(his[1]),
            s: parseInt(his[2])
        };
    }

    function timeToObj(time){
        now = new Date();
        try{
            time = time.split(':');
            var hasSecond = !!time[2];
            var h = parseInt(time[0], 10),
                i = parseInt(time[1], 10);
            var s = 0;
            if(hasSecond){
                 s = parseInt(time[2], 10);
                s = (s >=0 && s < 60) ? s : sec(now);
            }
            h = (h >=0 && h < 24) ? h : hour(now);
            i = (i >=0 && i < 60) ? i : minu(now);
           
            return {
                h: h,
                i: i,
                s: s,
                hasSecond: hasSecond
            }
        }catch(e){
            return {
                h: util._h(now),
                i: util._i(now),
                s: util._s(now),
                hasSecond: false
            }
        }
    }

    /**
     * \u6839\u636e\u914d\u7f6e\u53c2\u6570\u751f\u6210\u5bf9\u5c31\u7684\u65f6\u95f4\u5e8f\u5217
     */
    function ready(){
        var that = this,
            opts = that.opts;
        if(that.isSupport){
            return;
        }
        switch(opts.preset){
            case 'date':
                dateReady.call(that, opts);
                break;
            case 'time':
                timeReady.call(that, opts);
                break;
            case 'week':
                weekReady.call(that, opts);
                break;
            case 'diy':
                diyReady.call(that, opts);
                break;
        }
    }

    function scrollItem(self, that){

        self.updateData = function(newMap){
            if(newMap.toString() == self.map.toString()){
                return;
            }
            self.map = newMap;
            if(self.key == 'm'){
                generateWheelItems(that, self, self.rv, self.key, self.ul);
            }else{
                generateWheelItems(that, self, self.v, self.key, self.ul);
            }
            self.xscroll.refresh();
        }
        return self;
    }

    function dateReady(opts){
        var that = this;
        var startYear = opts.minDate.getFullYear() || startYear;
        var endYear = opts.maxDate.getFullYear() || endYear;
        try{

            var timeObj = dateToObj(opts.date);

        }catch(e){
            var cDate = _.isDate(opts.date) ? opts.date : now;
            var timeObj = {
                y: util._y(cDate),
                m: util._m(cDate),
                rm: util._rm(cDate) + 1,
                d: util._d(cDate),
                h: util._h(cDate),
                i: util._i(cDate),
                s: util._s(cDate)
            };
        }
        that.loop = ['y', 'm', 'd'];
        that.monthDay = getMaxDayOfMonth(timeObj.y, timeObj.m);
        that.y = new scrollItem({
            top: indexToTop(timeObj.y - startYear),
            v: timeObj.y,
            ov: timeObj.y,
            st: startYear,
            et: endYear,
            lis: [],
            map: makeArray(startYear, endYear)
        });
        that.m = new scrollItem({
            top: indexToTop(timeObj.rm - 1),
            v: timeObj.m,
            ov: timeObj.m,
            rv: timeObj.rm,
            orv: timeObj.rm,
            st: 1, et: 12,
            lis: [],
            map: makeArray(1, 12)
        });
        that.d = new scrollItem({
            top: indexToTop(timeObj.d - 1),
            v: timeObj.d,
            ov: timeObj.d,
            st: 1, et: 31,
            lis: [],
            map: makeArray(1, 31)
        });

        that.html = _generateContent.call(that, 'date', that.loop);
    }

    function timeReady(opts){

        var that = this;
        var timeObj = timeToObj(opts.date);
        if(timeObj.hasSecond){
            that.loop = ['h', 'i', 's'];
        }else{
            that.loop = ['h', 'i'];
        }
        that.loop.forEach(function(key){
            if(key == 'h'){
                var map = makeArray(0, 23);
            }else if(key == 'i' || key == 's'){
                var map = makeArray(0, 59);
            }
            that[key] = new scrollItem({
                top: indexToTop(timeObj[key] - 0),
                v: timeObj[key],
                index: valueToIndex(map, timeObj[key]),
                lis: [],
                map: map
            });
        });
        that.html = _generateContent.call(that, 'time', that.loop);
    }

    function weekReady(opts){
        var that = this;
        var timeObj = timeToObj(opts.date);
        that.w = new scrollItem({
            v: {0: false, 1: false, 2: false, 3: false, 4: false, 5: false, 6: false},
            ul: '',
            lis: [],
            map: makeArray(0, 6)
        });
        that.html = _generateContent.call(that, 'week');
        $$(that.w.ul).on('click', function(e){
            e.stopImmediatePropagation();
            var el = $$(e.target);
            if(el[0].tagName.toLowerCase() != 'li'){
                return;
            }
            var index = el.attr('data-index');
            var li = that.w.lis[index];
            li.toggleClass(SELECTED_CLS);
            that.w.v[index] = !that.w.v[index];
        });
    }

    function diyReady(opts){
        var that = this;
        if(!opts.data.length){
            return;
        }
        var data = opts.data;
        that.loop.length = 0;
        data.forEach(function(item){
            that[item.key] = new scrollItem({
                key: item.key,
                v: item.value,
                index: 0,
                oIndex: 0,
                et: item.resource.length - 1,
                ul: '',
                lis: [],
                map: item.resource
            }, that);
            var valueIndex = valueToIndex(item.resource, item.value);
            that[item.key]['index'] = valueIndex;
            that[item.key]['top'] = indexToTop(valueIndex);
            unit[item.key] = item.unit;
            that.loop.push(item.key);
        });
        that.html = _generateContent.call(that, 'diy', that.loop);

    }

    function makeArray(start, end){
        var arr = [];
        for(var i = start; i <= end; i ++){
            arr.push(i);
        }
        return arr;
    }
    var generateWheelItems = function(that, item, value, key, ul){
        var map = item.map,
            currentIndex = valueToIndex(item.map, value);
            len = map.length, start = 0, end = len -1;
        var height = len * 40 + 'px;';
        ul.style.height = height;
        
        ul.innerHTML = '';
        var itemHtml = '';
        var _unit = unit[key];

        for(var i = 0; i < 2; i++){
            ul.appendChild(createElement('<li></li>'));
        }

        that[key]['lis'].length = 0;

        for(var i = 0; i < len; i++){
            if(currentIndex === i){
                itemHtml = '<li class="selected">' + map[i] + ' ' + _unit + '</li>';
            }else{
                itemHtml = '<li>' + map[i] + ' ' + _unit + '</li>';
            }
            var li = createElement(itemHtml);
            that[key]['lis'].push(li);
            ul.appendChild(li);
        }
        that[key].lihook = createElement('<li></li>');
        ul.appendChild(that[key].lihook);
        ul.appendChild(createElement('<li></li>'));
        return ul;
    }

    function _generateContent(type, keys){
        var that = this,
            adw = createElement('<div class="ui-datetime-wrap" style="height: '+that.height+'px;"></div>');
        that.adw = adw;
        var line_item_top = ((that.height / 40) - 1) / 2 * 40;
        var line = createElement('<div class="ui-datetime-line" style="top: '+line_item_top+'px;"></div>');
        adw.appendChild(line);
        
        var generateWeek = function(){
            var ul = createElement('<ul class="ui-datetime-week"></ul>');
            week.forEach(function(w, i){
                var li = createElement('<li data-index="'+i+'">'+w+'</li>');
                ul.appendChild(li);
                that['w']['lis'].push($$(li));
            });
            that['w']['ul'] = ul;
            return ul;
        }
        var __generateWheelItems = function(item, value, key){
            var div = createElement('<div id="ui-datetime-'+that.guid+'-ad-'+key+'" class="ui-datetime-item" style="height:'+that.height+'px"></div>');
            var map = item.map, len = map.length;
            var height = len * 40 + 'px;';
            var ul = createElement('<ul style="'+height+'" class="xs-content"></ul>');
            generateWheelItems(that, item, value, key, ul);
            item.ul = ul;
            div.appendChild(ul);
            return div;
        }

        var generateDiy = function(){

            keys.forEach(function(key){
                var item = that[key];
                
                if(key == 'm'){
                    var itemHTML = __generateWheelItems(item, item.rv, key);
                }else{
                    var itemHTML = __generateWheelItems(item, item.v, key);
                }
                adw.appendChild(itemHTML);
            });
            return adw;
        }
        if(type == 'week'){
            return generateWeek();
        }else{
            return generateDiy();
        }
    }

    var NOOP = function(){};


    function bindEvent(el){
        var that = this;
        that.loop.forEach(function(item){
            if(!that[item]){
                return;
            }
            var snap = 0;
            var myScroll = new IScroll("#ui-datetime-"+that.guid+"-ad-" + item, { 
                bounceEasing: 'ease', 
                bounceTime: 1200
            });
            myScroll.scrollToIng = true;
            myScroll.scrollTo(0, that[item]['top'], 0, IScroll.utils.ease.circular, LOOP, 'correct');
            var timer = 0;
            myScroll.on("scrollEnd", function(__type){
                // if(this.scrollToIng){
                //     this.scrollToIng = false;
                // }
                var y = this.y;
                // console.log('y', y);
                // if(that[item].top == y){
                //     // that.syncStatus();
                //     return;
                // }
                snap = Math.round(y/40);
                if(that[item].top == y){
                    return;
                }
                that[item].top = snap * 40;
                that.changeValue(item);
                that.syncStatus();
                if(that.opts.onChange){
                    setTimeout(function(){
                        var currentValue = that.getTime();
                        var obj = {};
                        that.loop.forEach(function(_loop){
                            obj[_loop] = that[_loop];
                        })
                        that.opts.onChange.call(obj, currentValue);
                    }, 0);
                }

            });
            that[item].xscroll = myScroll;
        });
    }

    function topToIndex(height){
        return Math.abs(height) / 40;
    }

    function indexToTop(index){
        return 0 - index * 40;
    }

    function valueToIndex(map, value){
        // if(value === undefined){
        //     return 0;
        // }
        var indexValue = 0;
        var v = value.toString();
        map.forEach(function(item, index){
            if(v == item.toString()){
                indexValue = index;
            }
        });
        return indexValue;
    }


    /**
     * \u68c0\u6d4b\u662f\u5426\u652f\u6301\u539f\u751f date date-time date-time-local
     * @param inputElem
     * @param name
     * @returns {boolean}
     */
    AlinkDate.prototype.caniuse = function(){
        var that = this, opts = that.opts;

        var inputElem = D.createElement('input');
        inputElem.type = opts.preset;
        that.inputElem = inputElem;
        if(_.contains(presets, opts.preset)){
            inputElem.setAttribute('type', opts.preset);
//          return inputElem.type !== 'text';
            that.isSupport = false;
        }else{
            that.isSupport = false;
        }
        return that.isSupport;
    };
    AlinkDate.prototype.setData = function(value){
        var that = this;

        if(!value){
            value = that._syncTime();
        }
        that.loop.forEach(function(item){
            if(!that[item] || value[item] === undefined){
                return;
            }
            var obj = that[item];
            obj.ov = that[item].v;
            obj.v = value[item];
            obj.index = valueToIndex(that[item].map, obj.v);
            obj.oIndex = valueToIndex(that[item].map, obj.ov);
            obj.top = indexToTop(obj.index);
        });
        that.syncStatus();
        that.syncScroll();
    }
    AlinkDate.prototype.setTime = function(time){
        var that = this;
        if(that.opts.preset == 'diy'){
            return;
        }
        var flag = 1;
        if(!time){
            var currentTime = that._syncTime();
            flag = 1;
        }else if(_.isDate(time)){
            flag = 2;
        }else if(typeof(time) === 'string'){
            flag = 3;
            if(that.opts.preset == 'date'){
                var timeObj = dateToObj(time);
            }else{
                var timeObj = timeToObj(time);
            }
        }

        that.loop.forEach(function(item){
            if(!that[item]){
                return;
            }
            var obj = that[item];
            obj.ov = that[item].v;
            if(item === 'm'){
                obj.orv = that[item].rv;
            }
            if(flag === 1){
                obj.v = currentTime[item];
                if(item === 'm'){
                    obj.rv = currentTime['rm'];
                    obj.index = valueToIndex(obj.map, obj.rv);
                    obj.top = indexToTop(obj.index);
                }else{
                    obj.index = valueToIndex(obj.map, obj.v);
                    obj.top = indexToTop(obj.index);
                }
                return;
            }else if(flag === 2){
                obj.v = util['_' + item](time);
                if(item == 'm'){
                    obj.rv = util._rm(time);
                    obj.index = valueToIndex(obj.map, obj.rv);
                    obj.top = indexToTop(obj.index);
                    return;
                }else{
                    obj.index = valueToIndex(obj.map, obj.v);
                    obj.top = indexToTop(obj.index);
                }
                return;
            }else if(flag === 3){
                obj.v = timeObj[item];
                if(item == 'm'){
                    obj.rv = timeObj.rm;
                    obj.index = valueToIndex(obj.map, obj.rv);
                    obj.top = indexToTop(obj.index);
                }else{
                    obj.index = valueToIndex(obj.map, obj.v);
                    obj.top = indexToTop(obj.index);
                }
                return;
            }
        });
        that.syncStatus();
        that.syncScroll();
    };
    AlinkDate.prototype.getTime = function(){
        var that = this;

        var time = {};
        that.loop.forEach(function(item){
            if(!that[item]){
                return;
            }

            time[item] = that[item].v;
            if(item === 'm'){
                time.rm = that[item].rv;
            }
        })
        return time;
    }

    AlinkDate.prototype.getWeek = function(){
        var v = this.w.v;
        var value = [];
        for(var i in v){
            if(v[i]){
                value.push(parseInt(i, 10) + 1);
            }
        }
        return value;
    }
    AlinkDate.prototype.setWeek = function(week){
        var that = this, v = that.w.v, lis = that.w.lis, length = week.length, week_map = {};

        for(var j = 0; j < length; j++){
            week_map[week[j] - 1] = true;
        }
        for(var i= 0; i < 7; i++){
            if(week_map[i] === undefined){
                v[i] = false;
                lis[i].removeClass(SELECTED_CLS);
            }else{
                v[i] = true;
                lis[i].addClass(SELECTED_CLS);
            }
        }
    }
    AlinkDate.prototype._syncTime = function(){
        var that = this;
        var obj = {};
        that.loop.forEach(function(item){
            if(!that[item]){
                return ;
            }
            var top = that[item].top,
                value = that[item].map[topToIndex(top)];
            obj[item] = value;
            if(item === 'm'){
                obj['rm'] = valueToIndex(that[item].map, value);
                obj['m'] = obj['rm'] - 1;
            }
        });
        return obj;
    };

    AlinkDate.prototype.correctDayHTML = function(fromMonthDay, toMonthDay){
        var that = this;
        var diff = Math.abs(fromMonthDay - toMonthDay);
        if(!that.m){
            return;
        }

        if(fromMonthDay < toMonthDay){ // add
            for(var i = fromMonthDay; i < toMonthDay; i++){
                $$(that.d.lis[i]).insertBefore(that.d.lihook);
            }
        }else if(fromMonthDay > toMonthDay){ // remove

            for(var i = fromMonthDay; i > toMonthDay; i--){
                that.d.lis[i - 1].remove();
            }
        }
        that.monthDay = toMonthDay;
    }

    AlinkDate.prototype.changeValue = function(item){
        var that = this;
        if(that.m || that.h){
            that.setTime();
        }else{
            that.setData();
            return;
        }

        if(item == 'd'){
            return;
        }
        if(!that.m){
            return;
        }
        var monthOfDay = getMaxDayOfMonth(that.y.v, that.m.rv - 1);

        if(that.monthDay !== monthOfDay){
            that.correctDayHTML(that.monthDay, monthOfDay);
            that.d.xscroll.refresh();
        }
    }
    AlinkDate.prototype.syncScroll = function(){
        var that = this;
        var day_top = 0;
        that.loop.forEach(function(item){
            if(!that[item]){
                return ;
            }
            var obj = that[item];
            if(item === 'm'){
                var top = indexToTop(valueToIndex(obj.map, obj.rv));
            }else{
                var top = indexToTop(valueToIndex(obj.map, obj.v));
            }
            if(item === 'd'){
                day_top = top;
            }
            obj.xscroll.scrollToIng = true;
            obj.xscroll.scrollTo(0, top, 300, IScroll.utils.ease.circular, LOOP, 'correct');

        });

        if(that['m']){
            var monthOfDay = getMaxDayOfMonth(that.y.v, that.m.rv - 1);
            var dobj = that.d;
            if(that.monthDay !== monthOfDay){
                that.correctDayHTML(that.monthDay, monthOfDay);
                dobj.xscroll.refresh();
                dobj.scrollToIng = true;
                dobj.xscroll.scrollTo(0, day_top, 300, IScroll.utils.ease.circular, LOOP, 'correct');
            }else{
                dobj.scrollToIng = true;
                dobj.xscroll.scrollTo(0, day_top, 300, IScroll.utils.ease.circular, LOOP, 'correct');
            }
        }

    }
    AlinkDate.prototype.syncStatus = function(){
        var that = this;
        that.loop.forEach(function(item){
            if(!that[item]){
                return ;
            }
            var obj = that[item];
            if(item === 'm'){
                var oldIndex = valueToIndex(obj.map, obj.orv),
                    newIndex = valueToIndex(obj.map, obj.rv);
            }else{
                var oldIndex = valueToIndex(obj.map, obj.ov),
                    newIndex = valueToIndex(obj.map, obj.v);
            }
//            that[item].oIndex = oldIndex;
//            that[item].index = newIndex;
            if(oldIndex != newIndex){
                obj.lis[oldIndex].className = '';
                obj.lis[newIndex].className = 'selected';
            }
        });
    }

    AlinkDate.prototype.setDisabled = function(){
        var that = this;
        that.loop.forEach(function(item){
            that[item].xscroll.disable();
        });
    }
    AlinkDate.prototype.setEnabled = function(){
        var that = this;
        that.loop.forEach(function(item){
            that[item].xscroll.enable();
        });
    }

    AlinkDate.prototype.render = function(el){
        var that = this;

        if(that.isSupport){
            el.append(inputElem);
        }else{
            el.html('');
            el.append(that.html);
            that.correctDayHTML(31, that.monthDay);
            bindEvent.call(that, el);
        }
    };

    DA.Datetime = AlinkDate;
    return AlinkDate;
});



define('text!components/mode/mode.text.html',[],function () { return '<ul class="ui-mode" data-key="<%=key%>">\n    <%for(var i = 0, len = map.length; i < len; i++) { %>\n    <li data-value="<%=map[i].value%>" data-index="<%=i%>">\n        <i class="symbol"><%=map[i].text%></i>\n    </li>\n    <% } %>\n</ul>';});

define('text!components/mode/mode.icon.html',[],function () { return '<ul class="ui-mode" data-key="<%=key%>">\n    <%for(var i = 0, len = map.length; i < len; i++) { %>\n    <li data-value="<%=map[i].value%>" data-index="<%=i%>">\n        <i class="iconfont symbol"><%=map[i].icon%></i>\n        <em class="desc"><%=map[i].text%></em>\n    </li>\n    <% } %>\n</ul>';});

define('mode',['UIView', 'text!./components/mode/mode.text.html', 'text!./components/mode/mode.icon.html'
  ], function (UIView, textTemplate, iconTemplate) {

  var Mode = _.inherit(UIView, {
    propertys: function ($super) {
      $super();
      //html\u6a21\u677f

      this._disabled = false;
      this.datamodel = {
        currentValue: null
      };

      this.needRootWrapper = false;

      this.events = {
        'click li': 'clickAction'
      };
    },

    initialize: function ($super, name, opts) {
      opts.disabled = this.disabled;
      opts.enabled = this.enabled;
      this.uiname = name;
      $super(opts);
      
    },
    setTemplate: function(){
      if(this.tpl == 'text'){
        this.template = textTemplate;
        // this.uiStyle = textStyle;
      }else if(_.isFunction(this.tpl)){
        // this.uiStyle = iconStyle;
          this.template = this.tpl();
      }else if(this.tpl == 'icon'){
          this.template = iconTemplate;
      }else{
          this.template = this.tpl.toString();
      }
    },

    initElement: function () {
      this.el = this.$('.ui-mode');
      this.show();
      this.updateValue(this.datamodel.value);
      this.register(this.uiname, this);
    },

    onItemClick: function (item, index, e) {
      
    },

    setDeviceStatus: function(uuid, data){
      uuid = uuid || DA.uuid;
      if(!data){
          data = {};          
          data[this.datamodel.key] = { value : this.datamodel.value};
      }
      DA.setDeviceStatus(uuid, data);
    },

    //\u8fd9\u91cc\u4e0d\u4ee5dom\u5224\u65ad\uff0c\u4ee5\u5185\u7f6e\u53d8\u91cf\u5224\u65ad
    getValue: function () {
      return this.datamodel.value;
    },
    updateValue: function(value){
        this.$('.ui-mode li').removeClass('ui-mode-on');
        this.$('.ui-mode li[data-value="' + value + '"]').addClass('ui-mode-on');
        this.datamodel.value = value;
    },

    clickAction: function (e) {
        var el = $(e.currentTarget);
        var index = el.attr('data-index');
        var value = el.attr('data-value');
        // \u5f53\u524d\u4e0b\u53d1\u4e0e\u73b0\u5728\u5f53\u524d\u503c\u76f8\u7b49\uff0c\u5219\u4e0d\u5728\u4e0b\u53d1

        if(this.datamodel.value === value){
            if(!!!this.force_send){ // \u5982\u679c\u8981\u5f3a\u5236\u4e0b\u53d1\uff0c\u5219\u4e0d\u76f4\u63a5\u8fd4\u56de\uff0c\u4ecd\u7136\u629b\u51faonItemClick\u8ba9\u7528\u6237\u53bb\u81ea\u5b9a\u4e49\u4e0b\u53d1
                return;
            }
        }else{
            if (typeof this.onClickBefore == 'function' && !this.onClickBefore()) {return;}

            this.$('.ui-mode li').removeClass('ui-mode-on');
            el.addClass('ui-mode-on');
            this.datamodel.value = value;
        }
        var item = this.datamodel.map[index];
        if (typeof this.onItemClick == 'function') this.onItemClick.call(this, item, index, e);
    }
  });
  DA.Mode = Mode;
  return Mode;

});
define('text!components/switch/switch.text.html',[],function () { return '<div class="ui-switch ui-switch-off" data-key="<%=key%>">\n    <a href="#" class="symbol"><%=text%></a>\n</div>';});

define('text!components/switch/switch.icon.html',[],function () { return '<div class="ui-switch ui-switch-off" data-key="<%=key%>">\n    <i class="iconfont-bg"></i>\n    <i class="iconfont symbol"><%=icon%></i>\n    <em><%=text%></em>\n</div>';});

define('switch',['UIView', 'text!./components/switch/switch.text.html', 'text!./components/switch/switch.icon.html'
  // ,'text!./components/switch/switch.text.css', 'text!./components/switch/switch.icon.css'
  ], function (UIView, textTemplate, iconTemplate, textStyle, iconStyle) {

  var Switch = _.inherit(UIView, {
    propertys: function ($super) {
      $super();
      //html\u6a21\u677f

      this._disabled = false;
      this.datamodel = {
        checkedFlag: false
      };

      this.needRootWrapper = false;

      this.events = {
        'click': 'clickAction'
      };
    },

    initialize: function ($super, name, opts) {
      opts.disabled = this.disabled;
      opts.enabled = this.enabled;
      this.uiname = name;
      $super(opts);
      
    },
    setTemplate: function(){
        if(this.tpl == 'text'){
            this.template = textTemplate;
            // this.uiStyle = textStyle;
        }else if(_.isFunction(this.tpl)){
            // this.uiStyle = iconStyle;
            this.template = this.tpl();
        }else if(this.tpl == 'icon'){
            this.template = iconTemplate;
        }else{
            this.template = this.tpl.toString();
        }
    },

    initElement: function () {
      this.el = this.$('.ui-switch');
      this.show();
      this.updateValue(this.datamodel.value);
      this.register(this.uiname, this);
    },

    changed: function (status) {
    },

    checked: function () {
      if (typeof this.checkAvailabe == 'function' && !this.checkAvailabe()) {
        return;
      }
      if (this.getStatus()) return;

      if (typeof this.onClickBefore == 'function' && !this.onClickBefore()) {
        return;
      }
      this.el.addClass('ui-switch-on');
      this.el.removeClass('ui-switch-off');

      this.datamodel.checkedFlag = true;
      this._triggerChanged();
    },
    unChecked: function () {
      if (typeof this.checkAvailabe == 'function' && !this.checkAvailabe()) {
        return;
      }
      if (!this.getStatus()) return;

      if (typeof this.onClickBefore == 'function' && !this.onClickBefore()) {
        return;
      }

      this.el.removeClass('ui-switch-on');
      this.el.addClass('ui-switch-off');

      this.datamodel.checkedFlag = false;
      this._triggerChanged();
    },
    setDeviceStatus: function(uuid, data){
      uuid = uuid || DA.uuid;
      if(!data){
          data = {};
          var current_status = this.getStatus();
          var value = current_status ? this.datamodel.map.on : this.datamodel.map.off;
          data[this.datamodel.key] = { value : value};
      }
      DA.setDeviceStatus(uuid, data);
    },
    updateValue: function(value){
      this.datamodel.value = value;
      if(value == this.datamodel.map['on']){
        this.datamodel.checkedFlag = true;
        this.el.addClass('ui-switch-on');
        this.el.removeClass('ui-switch-off');
      }else{
        this.datamodel.checkedFlag = false;
        this.el.removeClass('ui-switch-on');
        this.el.addClass('ui-switch-off');
      }
    },
    //
    getValue: function(){
        var current_status = this.getStatus();
        var value = current_status ? this.datamodel.map.on : this.datamodel.map.off;
        return value;
    },

    _triggerChanged: function () {
      if (typeof this.changed == 'function') this.changed.call(this, this.getStatus());
    },

    //\u8fd9\u91cc\u4e0d\u4ee5dom\u5224\u65ad\uff0c\u4ee5\u5185\u7f6e\u53d8\u91cf\u5224\u65ad
    getStatus: function () {
      return this.datamodel.checkedFlag;
    },

    clickAction: function () {
      // \u7ec4\u4ef6\u88ab\u7981\u7528\u65f6\uff0c\u4e0d\u80fd\u70b9\u51fb
      if(this._disabled){
        return;
      }
      this.datamodel.value = this.getValue();
      if (this.getStatus()) {
        this.unChecked();
      } else {
        this.checked();
      }
    },
    checkAvailabe: function(){
      return !!!this._disabled;
    },
    disabled: function(){
      this._disabled = true;
      this.el.removeClass('ui-switch-on').removeClass('ui-switch-off');
      this.el.addClass('ui-switch-disabled');
    },
    enabled: function(){
      this._disabled = false;
      this.el.removeClass('ui-switch-disabled');
      if(this.getStatus()){
        this.el.addClass('ui-switch-on');
      }else{
        this.el.addClass('ui-switch-off');
      }
    }

  });
  DA.Switch = Switch;
  return Switch;

});
define('halfbox',['UIView','underscore'], function (UIView,_) {
    var $$ = $;
    var dashboardMoveEvent = function(e){
        e.preventDefault();
        return false;
    };
    var LOOP = function(){};

    var default_opts = {
        height: 320,
        overlayClickClose: true,
        onOpenBefore: LOOP,      // 
        onOpenAfter: LOOP,
        onCloseBefore: LOOP,
        onCloseAfter: LOOP
    };
    var _muid = 0;
    var guid = function(){
        return 'halfbox-' + _muid++;
    }   
  var HalfBox = _.inherit(UIView, {
    propertys: function ($super) { 
      $super();
      this.domhook = $('<div class="ui-half-box-ct"></div>');
      $(document.body).append(this.domhook)
      //html\u6a21\u677f

      // this._disabled = false;
      // this.datamodel = {
      //   currentValue: null
      // };

      // this.needRootWrapper = false;

      // this.events = {
      //   'click li': 'clickAction'
      // };
    },

    initialize: function ($super, config, opts) {
      opts = _.defaults(opts, default_opts);
      $super(opts);
      //config = config || controller;
      config = _.defaults(config, default_opts);
      var self = this;
      self.guid = guid();
      self.canOverlayClickClose = config.overlayClickClose;
      $(".ui-halfbox-wrap").remove();
      $(".ui-halfbox-overlay").remove();
      self.handle = $('<div class="ui-halfbox-wrap"></div>')[0];
      self.overlay = $('<div class="ui-halfbox-overlay"></div>')[0]
      self.height = config.height + 'px';
      self.handle.style.height = 0;
      if(_.isString(config.content)){
        self.handle.innerHTML = config.content;
      }else{
        self.handle.appendChild(config.content);
      }
      this.domhook[0].appendChild(self.handle);
      this.domhook[0].appendChild(self.overlay);
      self.config = config;

      $$(self.overlay).on('click', function(){
          if(self.canOverlayClickClose){
              self.close();
          }
      })
      return self;
    },
    open:function  () {
        var self = this, config = self.config;
        if(config.onOpenBefore){
            config.onOpenBefore.call(self);
        }
        document.body.addEventListener('touchmove', dashboardMoveEvent, false);
        // document.body.addEventListener('touchstart', dashboardMoveEvent, false);
        self.domhook.show();
        self.overlay.style.zIndex = 8999;
        self.handle.style.height = self.height;
        self.overlay.style.opacity = 0.4;
        // self.overlay.style.display = 'block';
        if(config.onOpenAfter){
            setTimeout(function(){
                config.onOpenAfter.call(self);
            }, 500);
        }
        return self;
    },
    close:function  () {
        var self = this, config = self.config;
        //appFunc.enabledDashboard();
        document.body.removeEventListener('touchmove', dashboardMoveEvent, false);
        // document.body.removeEventListener('touchstart', dashboardMoveEvent, false);
        var canClose = true;
        if(config.onCloseBefore){
            canClose = config.onCloseBefore.call(self) !== false;
        }
        if(!canClose){
            return;
        }
        // self.overlay.style.opacity = 0;
        // self.overlay.style.zIndex = -1;
        // self.handle.style.height = 0;
        // self.handle.style.display = 'none';
        self.domhook.remove()
        if(config.onCloseAfter){
            setTimeout(function(){
                config.onCloseAfter.call(self);
            }, 500);
        }
    },
    setCanOverlayClickClose : function(flag){
        self.canOverlayClickClose = flag;
    },
    setHeight : function(height){
            var self = this;
            self.height = height + 'px';
            self.handle.style.height = self.height;
    },
    getHandle : function(){
            return this.handle;
    },
    setTemplate: function(){
      // if(this.tpl == 'text'){
      //   this.template = textTemplate;
      //   // this.uiStyle = textStyle;
      // }else if(_.isFunction(this.tpl)){
      //   // this.uiStyle = iconStyle;
      //     this.template = this.tpl();
      // }else if(this.tpl == 'icon'){
      //     this.template = iconTemplate;
      // }else{
      //     this.template = this.tpl.toString();
      // }
    },

    initElement: function () {
      // this.el = this.$('.ui-switch');
      // this.show();
      // this.updateValue(this.datamodel.value);
      // this.register(this.uiname, this);
    },

    clickAction: function (e) {
        
    }
  });
  DA.HalfBox = HalfBox;
  return HalfBox;

});
define('pipsSlider-component',['UIView','underscore'], function(UIView,_) {
	var $$ = $,
        doc = $(document);
    var default_opts = {
        element: "",
        state: "off",
        min: 0,
        max: 100,
        value: 50,
        step: 1,
        onInit: function() {},
        onSlide: function(position, value) {},
        onSlideEnd: function(position, value) {},
        onTouchEnd: function(position, value) {}
    };
    var NOOP = function(){};

    var createElement = function(html){
        var tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        if (tempDiv.firstChild) {
            return tempDiv.firstChild;
        }
        return false;
    }
    var _uuid = 0;
    var uuid = function(){
        var u = Math.random().toString(36);
        return 'alinkpipslider' + u;
    }

    function getHiddenParentNodes(element) {
        var parents = [],
            node = element.parentNode;

        while (isHidden(node)) {
            parents.push(node);
            node = node.parentNode;
        }
        return parents;
    }
    function isHidden(element) {
        if(!element){
            return false;
        }
        if (element.offsetWidth !== 0 || element.offsetHeight !== 0) {
            return false;
        }
        return true;
    }

    function getDimension(element, key) {
        var hiddenParentNodes       = getHiddenParentNodes(element),
            hiddenParentNodesLength = hiddenParentNodes.length,
            displayProperty         = [],
            dimension               = element[key];

        if (hiddenParentNodesLength) {
            for (var i = 0; i < hiddenParentNodesLength; i++) {
                // Cache the display property to restore it later.
                displayProperty[i] = hiddenParentNodes[i].style.display;

                hiddenParentNodes[i].style.display = 'block';
                hiddenParentNodes[i].style.height = '0';
                hiddenParentNodes[i].style.overflow = 'hidden';
                hiddenParentNodes[i].style.visibility = 'hidden';
            }

            dimension = element[key];

            for (var j = 0; j < hiddenParentNodesLength; j++) {
                hiddenParentNodes[j].style.display = displayProperty[j];
                hiddenParentNodes[j].style.height = '';
                hiddenParentNodes[j].style.overflow = '';
                hiddenParentNodes[j].style.visibility = '';
            }
        }
        return dimension;
    }
    DA.AlinkPipsSlider = _.inherit(UIView,{
        initialize:function  ($super, config, opts) {
            //$super(opts);
            var self = this;
            opts = _.defaults(opts, default_opts);
            self.opts = opts;

            self.startEvent = 'touchstart'; //['mousedown', 'touchstart', 'pointerdown'];
            self.moveEvent  = 'touchmove'; //['mousemove', 'touchmove', 'pointermove'];
            self.endEvent   = 'touchend'; //['mouseup', 'touchend', 'pointerup'];
            self.cancelEvent = 'touchcancel';
            var cls =  'slider-'+Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
            self.onSlide    = opts.onSlide;
            self.onSlideEnd = opts.onSlideEnd;
            self.onSlideStart = opts.onSlideStart;
            self.onTouchEnd = opts.onTouchEnd;

            self.uuid       =     uuid();
            self.min        = parseFloat(opts.min || 0);
            self.max        = parseFloat(opts.max || 100);
            self.value      = parseFloat(opts.value || self.min);
            self.step       = parseFloat(opts.step || 1);
            self.pipsValue  = opts.pipsValue || [1, 2, 3];
            self.pipsDesc   = opts.pipsDesc || ["\u5c0f", "\u4e2d", "\u5927", "\u81ea\u52a8"];
            self.unit       = opts.unit || "";
            self.disabled   = opts.disabled || false;
            self.mutex      = opts.mutex || [];
            self.pipsStart  = opts.pipsStart || 0;
            self.enabledPips= opts.enabledPips || [];
            self.el         = $(opts.element);

            self.sliderLabel = $('<div class="ui-slider-label">' + opts.sliderLabel +'</div>');
            self.sliderCont  = $('<div class="ui-slider-cont"></div>');
            self.range       = $('<div class="ui-slider-range" />');
            self.handle      = $('<div class="ui-slider-handle" />');
            self.sliderValue = $('<div class="ui-slider-value"></div>');
            self.slider      = $('<div class="ui-slider-bar '+cls+'" id="' + self.uuid + '" />');
            self.sliderbg      = $('<div class="ui-slider-bg" />');
            self.sliderMin   = $('<div class="ui-slider-min-value">' + self.pipsDesc[self.min] + '</div>');
            self.sliderMax   = $('<div class="ui-slider-max-value">' + self.pipsDesc[self.max] + '</div>');

            self.slider.append(self.sliderbg);
            self.slider.append(self.range);
            self.slider.append(self.handle);
            self.slider.append(self.sliderValue);
            self.sliderCont.append(self.slider);
            self.sliderCont.append(self.sliderMin);
            self.sliderCont.append(self.sliderMax);

            self.el.append(self.sliderLabel);
            self.el.append(self.sliderCont);

            // Store context
            this.handleDown = _.bind(this.handleDown, this);
            this.handleMove = _.bind(this.handleMove, this);
            this.handleEnd  = _.bind(this.handleEnd, this);
            self.init();
            self.setPips();
            //add event
            doc.on(self.startEvent, '.'+cls, self.handleDown);
        },
        offEvent : function(){
            var self = this;
            doc.off(self.moveEvent, self.handleMove);
            doc.off(self.endEvent, self.handleEnd);
            doc.off(self.cancelEvent, self.handleCancel);
        },
        setPips : function() {
            var self = this;
            var str = '<div class="pips J_pip-scale">';
            var pipsScales;
            var pipsLength = self.pipsValue.length;
            for (var i = 0; i < pipsLength - 2; i++) {
                str += '<i class="pip-scale" style="width:' + (self.slider.width() - pipsLength - 1) / (pipsLength - 1) + 'px"></i>';
            }
            str += '</div>';
            pipsScales = createElement(str);
            pipsScales = $(pipsScales);
            self.el.find(".J_pip-scale").remove();
            self.slider.append(pipsScales);
        },
        init : function(){
            if (this.onInit && typeof this.onInit === 'function') {
                this.onInit();
            }
            this.update();
        },
        update : function() {
            this.handleWidth    = getDimension(this.handle[0], 'offsetWidth');
            this.sliderWidth     = getDimension(this.slider[0], 'offsetWidth');
            this.grabX          = this.handleWidth / 2;
            this.position       = this.getPositionFromValue(this.value);

            // Consider disabled state
    //        if (this.el[0].disabled) {
    //            this.slider.addClass(this.options.disabledClass);
    //        } else {
    //            this.slider.removeClass(this.options.disabledClass);
    //        }

            this.setPosition(this.position);
            this.setPips();
        },
        getPositionFromValue : function(value) {
            var percentage, pos;
            percentage = (value - this.min)/(this.max - this.min);
            pos = percentage * this.sliderWidth;
            return pos;
        },
        cap : function(pos, min, max) {
            if (pos < min) { return min; }
            if (pos > max) { return max; }
            return pos;
        },
        getPositionFromNode : function(node) {
            var i = 0;
            while (node !== null) {
                i += node.offsetLeft;
                node = node.offsetParent;
            }
            return i;
        },
        getRelativePosition : function(e) {
            // Get the offset left relative to the viewport
            var rangeX  = this.slider[0].getBoundingClientRect().left,
                pageX   = 0;

    //        if (typeof e.pageX !== 'undefined') {
    //            pageX = e.pageX;
    //        }
    //        else if (typeof e.originalEvent.clientX !== 'undefined') {
    //            pageX = e.originalEvent.clientX;
    //        }
    //        else if (e.originalEvent.touches && e.originalEvent.touches[0] && typeof e.originalEvent.touches[0].clientX !== 'undefined') {
    //            pageX = e.originalEvent.touches[0].clientX;
    //        }
    //        else if(e.currentPoint && typeof e.currentPoint.x !== 'undefined') {
    //            pageX = e.currentPoint.x;
    //        }

            pageX = e.touches[0].pageX;

            return pageX - rangeX;
        },
        getPositionFromValue : function(value) {
            var percentage, pos;
            percentage = (value - this.min)/(this.max - this.min);
            pos = percentage * this.sliderWidth;
            return pos;
        },
        adjustValue : function(value){
            var self = this;
            var enabledPips = self.enabledPips;
            var isContains = false;
            if(enabledPips.length == 0) {
                return true;
            }
            for (var item in enabledPips) {
                if (enabledPips[item] == value) {
                    isContains = true;
                }
            }
            if(isContains == true){
                return true;
            } else {
                return false;
            }
        },
        getValueFromPosition : function(pos) {
            var self = this;
            var percentage, value;
            percentage = ((pos) / (this.sliderWidth || 1));
            value = this.step * Math.round(percentage * (this.max - this.min) / this.step) + this.min;

            return Number((value).toFixed(2));
        },
        setPosition : function(pos) {
            var value, left, self = this;
            // Snapping steps
            value = (self.getValueFromPosition(self.cap(pos, 0, self.sliderWidth)) / self.step) * self.step;
            left = self.getPositionFromValue(value);

            // Update ui

            self.range[0].style.width = left + 'px';
            self.handle[0].style.left = left + 'px';
            self.sliderValue[0].style.left = left + 'px';
            self.sliderValue[0].innerHTML = '<em>' + self.pipsDesc[value] + '</em>' + self.unit;
            var sliderValue = $(self.sliderValue[0])
            self.sliderValue[0].style.marginLeft = - sliderValue.width() / 2 + 'px';

            // Update globals
            self.position = left;
            self.value = value;

            if (self.onSlide && typeof self.onSlide === 'function') {
                self.onSlide(left, value);
            }
        },
        handleDown : function(e) {
            var self = this;
            //console.log('pipsslider handleDown',self.disabled);
            e.preventDefault();
            if(self.disabled == false) {
                doc.on(self.moveEvent, self.handleMove);
                doc.on(self.endEvent, self.handleEnd);
                doc.on(self.cancelEvent, self.handleCancel);
                //\u64cd\u4f5c\u65f6\u662f\u5426\u9700\u8981\u9501\u4f4f\u540c\u6b65
                if(self.opts.isLockAnsyc){
                    self.onSlideStart && self.onSlideStart();
                }
            } else {
                self.offEvent();
                return;
            }
            self.pipsStart = self.value;

            // If we click on the handle don't set the new position
            if ((' ' + e.target.className + ' ').replace(/[\n\t]/g, ' ').indexOf('slider-handle') > -1) {
                //self.offEvent();
                return;
            }

            var posX    = self.getRelativePosition(e),  //\u5f53\u524d\u624b\u6307\u79bbslidebar\u7684x\u5750\u6807
                rangeX  = self.slider[0].getBoundingClientRect().left, //slidebar\u7684x\u5750\u6807
                handleX = self.getPositionFromNode(self.handle[0]) - rangeX;  //\u5f53\u524dhandle\u79bbslidebar\u7684x\u5750\u6807
            self.setPosition(posX - self.grabX);
            
            if (posX >= handleX && posX < handleX + self.handleWidth) {
                self.grabX = posX - handleX;
            }
        },
        handleMove : function(e) {
        
            var self = this;
            e.preventDefault();
            var posX = self.getRelativePosition(e);
            self.setPosition(posX - self.grabX);
        },
        handleEnd : function(e) {
            //console.log('touchend');
            var self = this;
            e.preventDefault();
            var currentValue = self.value;
            var isPipsSupport = self.adjustValue(currentValue);
            if(isPipsSupport == false){
                var posX = self.getPositionFromValue(self.pipsStart);
                self.setPosition(posX - self.grabX);
                self.tip("\u8be5\u6a21\u5f0f\u4e0b\u4e0d\u80fd\u8bbe\u7f6e\u4e3a" + self.pipsDesc[currentValue]);
            }
            // \u5982\u679c\u503c\u672a\u53d8\uff0c\u5219\u4e0d\u8bf7\u6c42
            if(self.pipsStart != self.value){
                if (self.onSlideEnd && typeof self.onSlideEnd === 'function') {
                    self.onSlideEnd(self.position, self.pipsValue[self.value], self.opts);
                }
                if (self.onTouchEnd && typeof self.onTouchEnd === 'function') {
                    self.onTouchEnd(self.position, self.pipsValue[self.value], self.opts);
                }
            }
            else{
                self.setPosition(self.getPositionFromValue(self.pipsStart));
            }
            self.offEvent();
        },
        tip : function(str){
            DA.toast({
                message: str,
                cls: "cls",
                duration: 3000
            });
        },
        handleCancel : function(e){
            e.preventDefault();
            var self = this;
            var prePosition = self.getPositionFromValue(self.pipsStart);
            //console.log(prePosition);
            self.setPosition(prePosition);
            self.offEvent();
        }
    });
   



    return DA.AlinkPipsSlider;
});
/**
 * PipsSlider\u5f00\u5173\u7ec4\u4ef6
 */
define('pipsSlider',[ 'pipsSlider-component'], function(PipsSliderCompoent) {
	
	
    var doc = $(document);
    var default_opts = {
        pipsSliderName: "",
        pipsSliderLabel: "",
        type: "PipsSlider",
        state: "off",
        min: 0, //\u6700\u5c0f\u6321\u4f4d\u7684\u7d22\u5f15
        max: 2, //\u6700\u5927\u6321\u4f4d\u7684\u7d22\u5f15
        value: 2, //\u5f53\u524d\u6321\u4f4d\u7684\u7d22\u5f15
        pipsValue: [0, 1, 2],  //\u4f20\u9001\u7ed9\u8fdc\u7aef\u8bbe\u5907\uff0c\u8868\u793a\u6321\u4f4d\u7684\u6570\u636e\u683c\u5f0f
        pipsDesc: ["\u5c0f", "\u4e2d", "\u5927"],  //\u663e\u793a\u7ed9C\u7aef\u666e\u901a\u7528\u6237\u770b\u5f97\u6321\u4f4d\u63cf\u8ff0
        unit: "",
        disabled: false,
        unUseWarnText: "", //\u8bbe\u7f6e\u7279\u5b9a\u7684\u63d0\u9192\u8bed
        mutex: {}, //\u4e92\u65a5\u5173\u7cfb //\u4e92\u65a5\u5173\u7cfb
        isLockAnsyc: true
    };

    var createElement = function(html){
        var tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        if (tempDiv.firstChild) {
            return tempDiv.firstChild;
        }
        return false;
    }
    var init = function( config) {

        config = _.defaults(config, default_opts);
        

        var PipsSlider = function ( config) {
            var self = this;
            config.onSlideEnd  = this.onSlideEnd.bind(this);
            self.changed = config.changed || function(){};
            self.datamodel = config.datamodel 
            config.onSlideStart = self.onSlideStart;
            self.slider = new PipsSliderCompoent(null,config);
            self.el = self.slider.el;
            config.name && self.slider.register(config.name,this);
            this.uiname = config.name;
            return self;
        };
        //PipsSlider.prototype = new BaseModel( config);
        PipsSlider.prototype.setValue = function(value){
            var self = this;
            var pipsValue = self.slider.pipsValue;

            for (var item in pipsValue) {
                console.log(self.datamodel.isDisabledSetValue)
                if(self.datamodel.isDisabledSetValue){
                    if (pipsValue[item] == value) {
                        self.slider.value = item;
                    }
                }
                if (pipsValue[item] == value && self.slider.disabled == false) {
                    self.slider.value = item;
                }

            }
            self.value = self.slider.value;
            self.slider.update();
        }
        
        PipsSlider.prototype.setDeviceStatus = function(uuid, data){
          uuid = uuid || DA.uuid;
          if(!data){
              data = {};          
              data[this.datamodel.key] = { value : this.datamodel.value};
          }
          DA.setDeviceStatus(uuid, data);
        }
        PipsSlider.prototype.setEnabledPips = function(){
            var self = this;
            var arg = arguments;
            var enabledValues = [];
            var pipsValue = self.slider.pipsValue;

            for (var item in pipsValue) {
                for(var i in arg){
                    if (pipsValue[item] == arg[i]) {
                        enabledValues[i] = item;
                    }
                }
            }
            self.slider.enabledPips = enabledValues;
        }
        PipsSlider.prototype.getValue = function(){
            return this.slider.pipsValue[this.slider.value];
        }
        PipsSlider.prototype.getPipsStart = function(){
            return this.slider.pipsStart;
        }
        PipsSlider.prototype.disabled = function(){
            var self = this;
            self.el.addClass('slider-disabled');
            self.isDisabled = true;
            self.off();
        }

        PipsSlider.prototype.resize = function(){
            var self = this;
            self.slider.value = self.value;
            self.slider.update();
        }

        PipsSlider.prototype.enabled = function(){
            var self = this;
            self.el.removeClass('slider-disabled');
            self.isDisabled = false;
            self.on();
        }
        PipsSlider.prototype.off = function(){
            var self = this;
            self.slider.disabled = true;
        }
        PipsSlider.prototype.on = function(){
            var self = this;
            self.slider.disabled = false;
        }
        PipsSlider.prototype.onSlideStart = function(){
            document._appControlling = true;
        }
        PipsSlider.prototype.onSlideEnd = function(left,value,obj){
            this.datamodel.value = value;
            this.changed(value) 
        }
        //\u624b\u6307\u79bb\u5f00
        PipsSlider.prototype.onTouchEnd = function(position, value){
            if(this.opts.onTouchEnd){
                this.opts.onTouchEnd(position, value);
            }
        }
        return new PipsSlider( config);
    };
    return DA.PipsSlider = init;
});

define('slider-component',['UIView','underscore'], function(UIView,_) {
	
	var doc = $(document);
    var default_opts = {
        element: "",
        state: "off",
        min: 0,
        max: 100,
        value: 50,
        step: 1,
        onInit: function() {},
        onSlide: function(position, value) {},
        onSlideEnd: function(position, value) {},
        onTouchEnd: function(position, value) {}
    };
    var NOOP = function(){};

    var createElement = function(html){
        var tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        if (tempDiv.firstChild) {
            return tempDiv.firstChild;
        }
        return false;
    }
    var _uuid = 0;
    var uuid = function(){
        var u = Math.random().toString(36);
        return 'alinkslider' + u;
    }

    function getHiddenParentNodes(element) {
        var parents = [],
            node = element.parentNode;

        while (isHidden(node)) {
            parents.push(node);
            node = node.parentNode;
        }
        return parents;
    }
    function isHidden(element) {
        if(!element){
            return false;
        }
        if (element.offsetWidth !== 0 || element.offsetHeight !== 0) {
            return false;
        }
        return true;
    }

    function getDimension(element, key) {
        var hiddenParentNodes       = getHiddenParentNodes(element),
            hiddenParentNodesLength = hiddenParentNodes.length,
            displayProperty         = [],
            dimension               = element[key];

        if (hiddenParentNodesLength) {
            for (var i = 0; i < hiddenParentNodesLength; i++) {
                // Cache the display property to restore it later.
                displayProperty[i] = hiddenParentNodes[i].style.display;

                hiddenParentNodes[i].style.display = 'block';
                hiddenParentNodes[i].style.height = '0';
                hiddenParentNodes[i].style.overflow = 'hidden';
                hiddenParentNodes[i].style.visibility = 'hidden';
            }

            dimension = element[key];

            for (var j = 0; j < hiddenParentNodesLength; j++) {
                hiddenParentNodes[j].style.display = displayProperty[j];
                hiddenParentNodes[j].style.height = '';
                hiddenParentNodes[j].style.overflow = '';
                hiddenParentNodes[j].style.visibility = '';
            }
        }
        return dimension;
    }
    DA.AlinkSlider = _.inherit(UIView,
    {
        initialize:function  ($super, config, opts) {
            //$super(opts);
            var self = this;
            opts = _.defaults(opts, default_opts);
            self.opts = opts;
            var cls =  'slider-'+Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
            self.startEvent = 'touchstart'; //['mousedown', 'touchstart', 'pointerdown'];
            self.moveEvent  = 'touchmove'; //['mousemove', 'touchmove', 'pointermove'];
            self.endEvent   = 'touchend'; //['mouseup', 'touchend', 'pointerup'];

            self.onSlide    = opts.onSlide;
            self.onSlideStart = opts.onSlideStart;
            self.onSlideEnd = opts.onSlideEnd;
            self.onTouchEnd = opts.onTouchEnd;

            self.uuid       =     uuid();
            self.min        = parseFloat(opts.min || 0);
            self.max        = parseFloat(opts.max || 100);
            self.minDesc    = opts.minDesc;
            self.maxDesc    = opts.maxDesc;
            self.value      = parseFloat(opts.value || self.min + (self.max-self.min)/2);
            self.step       = parseFloat(opts.step || 1);
            self.unit       = opts.unit || "";
            self.disabled   = opts.disabled || false;
            self.el         = $(opts.element);
            var numberStepMap = self.step.toString().split('.');
            self.numberFix = numberStepMap[1] ? numberStepMap[1].length : 0;


            self.sliderLabel = $('<div class="ui-slider-label">' + opts.sliderLabel +'</div>');
            self.sliderCont  = $('<div class="ui-slider-cont"></div>');
            self.range       = $('<div class="ui-slider-range" />');
            self.handle      = $('<div class="ui-slider-handle" />');
            self.sliderValue = $('<div class="ui-slider-value"></div>');
            self.slider      = $('<div class="ui-slider-bar '+cls+'" id="' + self.uuid + '" />');
            self.sliderbg      = $('<div class="ui-slider-bg" />');
            self.sliderMinDesc = $('<div class="ui-slider-min-desc">' + self.minDesc + '</div>');
            self.sliderMaxDesc = $('<div class="ui-slider-max-desc">' + self.maxDesc + '</div>');
            self.sliderMin = $('<div class="ui-slider-min-value">' + self.min + '</div>');
            self.sliderMax = $('<div class="ui-slider-max-value">' + self.max + '</div>');




            self.slider.append(self.range);
            self.slider.append(self.sliderbg);
            self.slider.append(self.handle);
            self.sliderCont.append(self.slider);
            if(self.minDesc && self.maxDesc){
                self.sliderCont.append(self.sliderMinDesc);
                self.sliderCont.append(self.sliderMaxDesc);
            } else {
                self.slider.append(self.sliderValue);
                self.sliderCont.append(self.sliderMin);
                self.sliderCont.append(self.sliderMax);
            }
            self.el.append(self.sliderLabel);
            self.el.append(self.sliderCont);


            // Store context
            this.handleDown = _.bind(this.handleDown, this);
            this.handleMove = _.bind(this.handleMove, this);
            this.handleEnd  = _.bind(this.handleEnd, this);
            self.init();

            doc.on(self.startEvent, '.'+cls, self.handleDown);

        },

        updateConfig : function(config){
            var self = this, opts = self.opts;
            opts.min = config.min || opts.min;
            opts.max = config.max || opts.max;
            opts.minDesc = config.minDesc || opts.minDesc;
            opts.value = config.value || opts.value;
            opts.step = config.step || opts.step;
            opts.unit = config.unit || opts.unit;
            opts.disabled = config.disabled || opts.disabled;
            self.min        = parseFloat(opts.min || 0);
            self.max        = parseFloat(opts.max || 100);
            self.minDesc    = opts.minDesc;
            self.maxDesc    = opts.maxDesc;
            self.value      = parseFloat(opts.value || self.min + (self.max-self.min)/2);
            self.step       = parseFloat(opts.step || 1);
            self.unit       = opts.unit || "";
            self.disabled   = opts.disabled || false;

            var numberStepMap = self.step.toString().split('.');
            self.numberFix = numberStepMap[1] ? numberStepMap[1].length : 0;

            self.sliderMinDesc.text(self.minDesc);
            self.sliderMaxDesc.text(self.maxDesc);
            self.sliderMin.text(self.min);
            self.sliderMax.text(self.max);
            self.update();
        },
        init : function(){
            if (this.onInit && typeof this.onInit === 'function') {
                this.onInit();
            }
            this.update();
        },
        update : function() {
            this.handleWidth    = getDimension(this.handle[0], 'offsetWidth');
            this.sliderWidth     = getDimension(this.slider[0], 'offsetWidth');
            this.grabX          = this.handleWidth / 2;
            this.position       = this.getPositionFromValue(this.value);

            // Consider disabled state
    //        if (this.el[0].disabled) {
    //            this.slider.addClass(this.options.disabledClass);
    //        } else {
    //            this.slider.removeClass(this.options.disabledClass);
    //        }

            this.setPosition(this.position);
        },
        getPositionFromValue : function(value) {
            var percentage, pos;
            percentage = (value - this.min)/(this.max - this.min);
            pos = percentage * this.sliderWidth;
            return pos;
        },
        cap : function(pos, min, max) {
            if (pos < min) { return min; }
            if (pos > max) { return max; }
            return pos;
        },
        // Returns element position relative to the parent
        getPositionFromNode : function(node) {
            var i = 0;
            while (node !== null) {
                i += node.offsetLeft;
                node = node.offsetParent;
            }
            return i;
        },

        getRelativePosition : function(e) {
            // Get the offset left relative to the viewport
            var rangeX  = this.slider[0].getBoundingClientRect().left,

            pageX = e.touches[0].pageX;

            return pageX - rangeX;
        },

        getPositionFromValue : function(value) {
            var percentage, pos;
            percentage = (value - this.min)/(this.max - this.min);
            pos = percentage * this.sliderWidth;
            return pos;
        },

        getValueFromPosition : function(pos) {
            var percentage, value;
            percentage = ((pos) / (this.sliderWidth || 1));
            value = this.step * Math.round(percentage * (this.max - this.min) / this.step) + this.min;
            return Number((value).toFixed(2));
        },

        setPosition : function(pos) {
            var value, left, self = this;

            // Snapping steps
            value = (self.getValueFromPosition(self.cap(pos, 0, self.sliderWidth)) / self.step) * self.step;
            var num = new Number(value);
            value = parseFloat(num.toFixed(self.numberFix));
            left = self.getPositionFromValue(value);

            // Update ui
            self.range[0].style.width = left + 'px';
            self.handle[0].style.left = left + 'px';
            self.sliderValue[0].style.left = left + 'px';
            self.sliderValue[0].innerHTML = '<em>' + value + '</em>' + self.unit;
            var sliderValue = $(self.sliderValue[0])
            self.sliderValue[0].style.marginLeft = - sliderValue.width() / 2 + 'px';

            // Update globals
            self.position = left;
            self.value = value;

            if (self.onSlide && typeof self.onSlide === 'function') {
                self.onSlide(left, value);
            }
        },
        handleDown : function(e) {
            var self = this;
            e.preventDefault();
            if(self.disabled == false) {
                doc.on(self.moveEvent, self.handleMove);
                doc.on(self.endEvent, self.handleEnd);
                //\u64cd\u4f5c\u65f6\u662f\u5426\u9700\u8981\u9501\u4f4f\u540c\u6b65
                if(self.opts.isLockAnsyc){
                    self.onSlideStart && self.onSlideStart();
                }
            } else {
                doc.off(self.moveEvent, self.handleMove);
                doc.off(self.endEvent, self.handleEnd);
                return;
            }

            // If we click on the handle don't set the new position
            if ((' ' + e.target.className + ' ').replace(/[\n\t]/g, ' ').indexOf('slider-handle') > -1) {
                return;
            }

            var posX    = self.getRelativePosition(e),
                rangeX  = self.slider[0].getBoundingClientRect().left,
                handleX = self.getPositionFromNode(self.handle[0]) - rangeX;
            self.setPosition(posX - self.grabX);

            if (posX >= handleX && posX < handleX + self.handleWidth) {
                self.grabX = posX - handleX;
            }
        },

        handleMove : function(e) {
            var self = this;
            e.preventDefault();
            var posX = self.getRelativePosition(e);
            self.setPosition(posX - self.grabX);
        },


        handleEnd : function(e) {
            var self = this;
            e.preventDefault();
            doc.off(self.moveEvent, self.handleMove);
            doc.off(self.endEvent, self.handleEnd);

            if (self.onSlideEnd && typeof self.onSlideEnd === 'function') {
                self.onSlideEnd(self.position, self.value);
            }
            if (self.onTouchEnd && typeof self.onTouchEnd === 'function') {
                self.onTouchEnd(self.position, self.value);
            }
        }
    })
    return DA.AlinkSlider;
});
/**
 * Slider\u5f00\u5173\u7ec4\u4ef6
 */
define('slider',[ 'slider-component'], function( SliderCompoent) {
	

    var default_opts = {
        element: "",
        sliderName: "",
        sliderLabel: "",
        type: "slider",
        state: "off",
        minValue: 0,
        maxValue: 100,
        currentValue: 50,
        step: 5,
        unit: "\u2103",
        disabled: false,
        unUseWarnText: "", //\u8bbe\u7f6e\u7279\u5b9a\u7684\u63d0\u9192\u8bed
        mutex: [], //\u4e92\u65a5\u5173\u7cfb
        isLockAnsyc: true
    };

    var createElement = function(html){
        var tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        if (tempDiv.firstChild) {
            return tempDiv.firstChild;
        }
        return false;
    }
    var init = function( config) {

        config = _.defaults(config, default_opts);

        var Slider = function ( config) {
            var self = this;
            config.onSlideEnd = _.bind(self.onSlideEnd, this);
            config.onSlideStart = self.onSlideStart;
            self.changed = config.changed || function(){};
            self.datamodel = config.datamodel
            self.slider = new SliderCompoent(null,config);
            self.el = self.slider.el;
            self._disabledSetValue = false;
            config.name && self.slider.register(config.name,this);
            this.uiname = config.name;
            return self;
        };
        

        Slider.prototype.updateConfig = function(config){
            this.slider.updateConfig(config);
            if(config.value){
                this.value = config.value;
            }
        }
        Slider.prototype.setDeviceStatus = function(uuid, data){
          uuid = uuid || DA.uuid;
          if(!data){
              data = {};          
              data[this.datamodel.key] = { value : this.datamodel.value};
          }
          DA.setDeviceStatus(uuid, data);
        }
        Slider.prototype.setValue = function(value){
            var self = this;
            if(!self._disabledSetValue){
                self.slider.value = value;
                self.value = value;
                self.slider.update();
            }
        }
        Slider.prototype.disabledSetValue = function(){
            this._disabledSetValue = true;
        }
        Slider.prototype.disabled = function(){
            var self = this;
            self.el.addClass('slider-disabled');
            self.isDisabled = true;
            self.off();
        }

        Slider.prototype.resize = function(){
            var self = this;
            self.slider.value = self.value;
            self.slider.update();
        }

        Slider.prototype.enabled = function(){
            var self = this;
            self.el.removeClass('slider-disabled');
            self.isDisabled = false;
            self.on();
        }
        Slider.prototype.off = function(){
            var self = this;
            self.slider.disabled = true;
        }
        Slider.prototype.on = function(){
            var self = this;
            self.slider.disabled = false;
        }
        Slider.prototype.getValue = function(){
            return this.slider.value;
        }
        Slider.prototype.onSlideStart = function(){
            document._appControlling = true;

        }
        Slider.prototype.onSlideEnd = function(left,value,obj){
            this.datamodel.value = value;
            this.changed(value) 
        }
        //\u624b\u6307\u79bb\u5f00
        Slider.prototype.onTouchEnd = function(position, value){
            if(this.opts.onTouchEnd){
                this.opts.onTouchEnd(position, value);
            }
        }
        return new Slider( config);
    };
    return DA.Slider = init;
});

define('selectMode',['UIView','underscore'
  ], function (UIView,_) {
  var SelectMode = _.inherit(UIView, {
    

    initialize: function ($super, cfg, opts) {
    
      //$super(opts);
      this.$el = this.rootElement = $(cfg.rootElement);
      this.changed = cfg.changed ||  this.changed;
      this.bindEvents();
      cfg.name && this.register(cfg.name,this);
      this.uiname = cfg.name;
    },
    bindEvents:function  () {
      var delegateCls = this.$el[0].className  + Math.random().toString(36).substring(2);
      this.$el.addClass(delegateCls);
      $(document.body).on('click','.'+delegateCls+' .ui-select-mode-btn',_.bind(this.clickAction,this));
    }, 

    

    changed: function (status) {
    },

    switchBtn: function (current,datamode) {
      this._triggerChanged(current,datamode);
    },
    
    setDeviceStatus: function(uuid, data){
      uuid = uuid || DA.uuid;
      if(!data){ 
          var data = this.getStatus();
      } 
      DA.setDeviceStatus(uuid, data);
    },
    updateValue: function(index){
      this.rootElement.find('.ui-select-mode-btn').removeClass('current').eq(index).addClass('current');
    },
    //
    getValue: function(){ 
        var current_status = this.getStatus();
        var value = current_status ? this.datamodel.map.on : this.datamodel.map.off;
        return value;
    },
 
    _triggerChanged: function (current,datamode) {
      if (typeof this.changed == 'function') this.changed.apply(this, [current,datamode]);
    },

    //\u8fd9\u91cc\u4e0d\u4ee5dom\u5224\u65ad\uff0c\u4ee5\u5185\u7f6e\u53d8\u91cf\u5224\u65ad
    getStatus: function () {
      var attr = this.rootElement.find('.current').attr('data-mode');
      return JSON.parse(attr);
    },

    clickAction: function (e) {
      this.rootElement.find('.ui-select-mode-btn').removeClass('current');
      var current = $(e.currentTarget);
      var attr = current.attr('data-mode');
      this.datamode = JSON.parse(attr);
      current.addClass('current')
      this.switchBtn(current[0],this.datamode);
    }

  });
  var fn = function  (c) {
    if ( !(this instanceof SelectMode) ) {
      return new SelectMode( c );
    }
  }
  DA.SelectMode = fn;
  return fn;

});
define('__sdk_main__',[
    'zepto', 
    'windvane', 
    'text', 
    'css',  

    'fastclick',
    'underscore',
    'underscore_extend',
    'IScroll',

    '_sdk_alink',
    '_sdk_api',
    '_sdk_event',
    '_sdk_storage',
    '_sdk_tool',
    '_sdk_bletool',
    '_sdk_native_component',
    '_sdk_webview_data',

    'flipsnap',
    'UIView',
    'datetime',  
    "mode",
    "switch",
    'halfbox',
    'pipsSlider',
    'slider',
    'selectMode',

    //"css!./style/iconfont.css",
    //"css!./style/components-styles.css",
    // "css!./components/mode/ui.mode.css",
    // "css!./components/switch/ui.switch.css",
    // "css!./components/datetime/ui.datetime.css",
    // "css!./components/halfbox/ui.halfbox.css",
    ], function(){
    });
