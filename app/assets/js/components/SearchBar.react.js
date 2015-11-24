import React from 'react';
import {InputGroup, FormInput, Button} from 'elemental';
import {TextField} from 'material-ui';

export default class SearchBar extends React.Component {
    constructor(props) {
        super(props);
    }


    render() {
        return (
            <TextField
                className="home-search-bar"
                hintText={this.props.placeholder}
                value={this.props.currentQuery}
                //underlineStyle={{borderColor:Colors.green500}}
                onChange={this.props.handleChange}
                fullWidth={true}
                onKeyDown={this.props.handleEnter}
            />
        );


        //return (
        //    <InputGroup contiguous>
        //        <InputGroup.Section grow>
        //            <FormInput type="text" placeholder={this.props.placeholder}
        //                       value={this.props.currentQuery}
        //                       onChange={this.props.handleChange} onKeyDown={this.props.handleEnter}/>
        //        </InputGroup.Section>
        //        <InputGroup.Section>
        //            <Button type="primary">{this.props.buttonText}</Button>
        //        </InputGroup.Section>
        //    </InputGroup>
        //)
    }

}