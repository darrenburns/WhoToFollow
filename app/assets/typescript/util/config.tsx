export default class Configuration {
    // How often to send keep-alives in milliseconds
    public static get KEEP_ALIVE_FREQUENCY(): number { return 10*1000; }

    // The base URL to make Ajax requests to
    public static get BASE_SITE_URL(): string { return "http://localhost:9000/" }

}
