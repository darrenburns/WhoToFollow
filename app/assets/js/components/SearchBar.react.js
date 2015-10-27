import React from 'react';
import {InputGroup, FormInput, Button} from 'elemental';

class SearchBar extends React.Component {
    constructor(props) {
        super(props);
    }


    render() {
        return (
            <InputGroup contiguous>
                <InputGroup.Section grow>
                    <FormInput type="text" placeholder={this.props.placeholder}
                               value={this.props.currentQuery}
                               onChange={this.props.handleChange} onKeyDown={this.props.handleEnter}/>
                </InputGroup.Section>
                <InputGroup.Section>
                    <Button type="primary">{this.props.buttonText}</Button>
                </InputGroup.Section>
            </InputGroup>
        )
    }

}
module.exports = SearchBar;