import * as React from 'react';
import {Link} from 'react-router';

interface HashtagProps {
    key?: any;
    hashtag: string;
}

const Hashtag = React.createClass<HashtagProps, any>({
    render() {
        return (
            <span className="hashtag">
                <Link to={`/query/${this.props.hashtag}`}>#{this.props.hashtag}</Link>
            </span>
        )
    }
});

export default Hashtag;