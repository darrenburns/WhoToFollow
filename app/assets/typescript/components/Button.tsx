import * as React from 'react';
import * as tinycolor from 'tinycolor2';
import ReactChildren = __React.ReactChildren;

interface ButtonProps {
    label: string;
    doneLabel?: string;
    colour: string;
    backgroundColour: string;
    onClick: any;
    children?: ReactChildren
}

export default class Button extends React.Component<ButtonProps, any> {

    constructor(props) {
        super(props);
        this.state = {
            disabled: false
        }
    }

    _acceptOneClick = (): void => {
        this.props.onClick();
        this.setState({
            disabled: true
        })
    };

    render() {
        let buttonStyle = {
            padding: "4px",
            color: this.props.colour,
            backgroundColor: this.props.backgroundColour,
            border: "1px solid " + tinycolor(this.props.backgroundColour).darken(10).toHexString()
        };
        return (
            <button className="wtfic-button" disabled={this.state.disabled} style={buttonStyle} onClick={this._acceptOneClick}>
                {this.state.disabled ? this.props.doneLabel :
                    (this.props.label || this.props.children)}
            </button>
        )
    }

}