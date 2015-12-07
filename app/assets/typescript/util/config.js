var Configuration = (function () {
    function Configuration() {
    }
    Object.defineProperty(Configuration, "KEEP_ALIVE_FREQUENCY", {
        // How often to send keep-alives in milliseconds
        get: function () { return 15 * 1000; },
        enumerable: true,
        configurable: true
    });
    return Configuration;
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = Configuration;
