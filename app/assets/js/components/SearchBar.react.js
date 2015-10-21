import React from 'react';
import {InputGroup, FormInput, Button} from 'elemental';

const SearchBar = (props) => (
    <InputGroup contiguous>
        <InputGroup.Section grow>
            <FormInput type="text" placeholder={props.placeholder} />
        </InputGroup.Section>
        <InputGroup.Section>
            <Button type="primary">{props.buttonText}</Button>
        </InputGroup.Section>
    </InputGroup>
);

module.exports = SearchBar;