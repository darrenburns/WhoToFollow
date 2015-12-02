import React from 'react';
import {Link} from 'react-router';

const Hashtag = React.createClass({
    render() {
        return (
            <span className="hashtag">
                <Link to={`/query/${this.props.hashtag}`}>#{this.props.hashtag}</Link>
            </span>
        )
    }
});

export default Hashtag;