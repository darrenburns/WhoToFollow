export default class Logger {

    public static info(message: string, tag: string = ''): void {
        if (message === '') {
            return;
        }
        let output = '';
        if (tag !== '') {
            output += `[${tag}] `
        }
        output += message;
        console.log(output);
    }

}