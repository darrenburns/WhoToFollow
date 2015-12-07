import * as React from 'react';
import {TextField} from 'material-ui';

interface SearchBarProps {
    placeholder: string,
    currentQuery: string,
    handleChange: any,
    handleEnter: any
}

export default class SearchBar extends React.Component<SearchBarProps, any> {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <TextField
                hintText={this.props.placeholder}
                value={this.props.currentQuery}
                onChange={this.props.handleChange}
                fullWidth={true}
                onKeyDown={this.props.handleEnter}
            />
        );

    }

}